package sophon.desktop.feature.packetcapture.data.source.mitm

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoop
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import io.netty.util.concurrent.GenericFutureListener

/**
 * 管理单条 HTTP/2 后端连接的完整生命周期，在服务端发送 **GOAWAY** 帧（例如关闭长时间空闲的连接）
 * 后自动重建连接，使抓包管道对 gRPC App 端保持透明。
 *
 * ## 使用约定
 * - 所有公开方法**必须在同一 EventLoop 线程**上调用，因此内部状态无需额外同步。
 * - 初始连接由 [ProxyFrontendHandler] 建立并通过 [donateChannel] 移交；
 *   后续重连由本类在 [withChannel] 被调用时按需触发。
 *
 * ## 并发重连保护
 * 若多条前端流在同一时刻发现后端连接已失效，首个调用触发重连，后续调用均进入 [pendingActions]
 * 队列等待同一次重连结果，不会发起多余的 TCP 握手。
 *
 * @param host                          后端主机名（用于 TLS SNI 和重连地址）
 * @param port                          后端端口
 * @param clientSslCtx                  已配置好 ALPN（优先 h2）和 InsecureTrustManager 的客户端 SSL 上下文
 * @param downloadTrafficShapingHandler 后端下载限速 handler（可为 null），重连后自动注入新 channel 的 pipeline
 */
internal class BackendChannelManager(
    private val host: String,
    private val port: Int,
    private val clientSslCtx: SslContext,
    private val downloadTrafficShapingHandler: GlobalTrafficShapingHandler? = null,
) {

    private var currentChannel: Channel? = null
    private var isConnecting = false
    private val pendingActions = ArrayDeque<Pair<(Channel) -> Unit, (Throwable) -> Unit>>()

    /**
     * 将初始已建立的后端 Channel 移交给本管理器。
     *
     * 调用者（[ProxyFrontendHandler]）负责完成 TCP + TLS + h2 codec 的初始握手，
     * 然后调用此方法将所有权转移；本管理器仅负责监听 close 事件并在需要时重建。
     */
    fun donateChannel(channel: Channel) {
        currentChannel = channel
        channel.closeFuture().addListener { currentChannel = null }
    }

    /**
     * 获取可用的后端 Channel，若当前连接已失效则先**重建**再回调。
     *
     * 典型调用场景：[Http2FrontendStreamHandler] 在 [dispatchToBackend] 中为每条新的 gRPC 流
     * 调用此方法以获取可用的父 Channel，再通过 [io.netty.handler.codec.http2.Http2StreamChannelBootstrap]
     * 在其上开子流。
     *
     * @param eventLoop 调用方所在的 EventLoop；重连时在同一 EventLoop 上建立新连接
     * @param action    Channel 就绪后在 EventLoop 线程上执行的操作
     * @param onError   本次重连或后续操作失败时的回调
     */
    fun withChannel(
        eventLoop: EventLoop,
        action: (Channel) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val ch = currentChannel
        if (ch != null && ch.isActive) {
            action(ch)
            return
        }
        pendingActions += action to onError
        if (!isConnecting) {
            isConnecting = true
            reconnect(eventLoop)
        }
    }

    // -------------------------------------------------------------------------
    // 重连内部实现
    // -------------------------------------------------------------------------

    private fun reconnect(eventLoop: EventLoop) {
        Bootstrap()
            .group(eventLoop)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        "backendSsl",
                        clientSslCtx.newHandler(ch.alloc(), host, port)
                    )
                }
            })
            .connect(host, port)
            .addListener(ChannelFutureListener { connectFuture ->
                if (!connectFuture.isSuccess) {
                    flushPendingWithError(connectFuture.cause() ?: RuntimeException("Reconnect TCP failed"))
                    return@ChannelFutureListener
                }

                val newChannel = connectFuture.channel()

                // 防御性异常捕获器：覆盖握手完成到 h2 codec 就位前的空窗期
                newChannel.pipeline().addLast(
                    "backendErrCatcher",
                    object : ChannelInboundHandlerAdapter() {
                        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                            ctx.close()
                        }
                    }
                )

                val sslHandler = newChannel.pipeline().get(SslHandler::class.java)!!
                sslHandler.handshakeFuture().addListener(GenericFutureListener { handshakeFuture ->
                    if (!handshakeFuture.isSuccess) {
                        newChannel.close()
                        flushPendingWithError(
                            handshakeFuture.cause() ?: RuntimeException("Reconnect TLS handshake failed")
                        )
                        return@GenericFutureListener
                    }

                    // applicationProtocol() 是 SslHandler 的方法，从外层 val 访问
                    val proto = sslHandler.applicationProtocol()
                    if (proto != ApplicationProtocolNames.HTTP_2) {
                        newChannel.close()
                        flushPendingWithError(
                            IllegalStateException("Reconnected backend negotiated '$proto' instead of h2")
                        )
                        return@GenericFutureListener
                    }

                    // 时序关键：必须在 EventLoop 线程（此 listener 内）同步安装 h2 codec，
                    // 确保在服务端首个 SETTINGS 帧到达前管道已就位（与初始握手的约定相同）。
                    addHttp2BackendCodec(newChannel)

                    // 重连后端 channel 同样注入下载限速 handler，与初始连接保持一致。
                    downloadTrafficShapingHandler?.let { handler ->
                        newChannel.pipeline().addAfter("backendSsl", "backendDownloadThrottle", handler)
                    }

                    currentChannel = newChannel
                    newChannel.closeFuture().addListener(ChannelFutureListener { currentChannel = null })

                    isConnecting = false
                    val drained = pendingActions.toList()
                    pendingActions.clear()
                    drained.forEach { (action, _) -> action(newChannel) }
                })
            })
    }

    private fun flushPendingWithError(cause: Throwable) {
        isConnecting = false
        val drained = pendingActions.toList()
        pendingActions.clear()
        drained.forEach { (_, onError) -> onError(cause) }
    }
}
