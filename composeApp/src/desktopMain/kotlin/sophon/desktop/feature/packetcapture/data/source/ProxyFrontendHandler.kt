package sophon.desktop.feature.packetcapture.data.source

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sophon.desktop.feature.packetcapture.data.source.protocol.Http1MitmSession
import sophon.desktop.feature.packetcapture.data.source.protocol.Http2MitmSession
import sophon.desktop.feature.packetcapture.data.source.protocol.InboundRequest
import sophon.desktop.feature.packetcapture.data.source.protocol.MitmProtocol
import sophon.desktop.feature.packetcapture.data.source.protocol.ProtocolDetector
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.concurrent.atomic.AtomicLong

private const val MAX_CONTENT_LENGTH = 10 * 1024 * 1024

/**
 * Netty 前端入站处理器，根据 [ProtocolDetector] 的检测结果分发请求：
 * - [InboundRequest.PlainHttp]：直连后端并捕获完整请求/响应。
 * - [InboundRequest.ConnectTunnel]：在协程中顺序执行双向 TLS 握手、ALPN 检测，
 *   再委托对应 Session 类装配 MITM 管道。
 *
 * TLS 握手流程原有的四层回调嵌套，通过 [awaitChannel]/[awaitHandshake]/[awaitWrite]
 * 桥接为顺序 suspend 代码，由注入的 [scope] 调度执行。
 */
class ProxyFrontendHandler(
    private val scope: CoroutineScope,
    private val onPacketCaptured: (CapturedPacket) -> Unit,
    private val idCounter: AtomicLong
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        when (val request = ProtocolDetector.detect(msg)) {
            is InboundRequest.PlainHttp    -> handlePlainHttp(ctx, msg, request)
            is InboundRequest.ConnectTunnel -> scope.launch { handleConnect(ctx, request) }
        }
    }

    // -------------------------------------------------------------------------
    // CONNECT 隧道：TLS 握手顺序化（四层嵌套回调 → 线性 suspend 代码）
    // -------------------------------------------------------------------------

    private suspend fun handleConnect(ctx: ChannelHandlerContext, tunnel: InboundRequest.ConnectTunnel) {
        val clientSslCtx = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .applicationProtocolConfig(
                ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1
                )
            )
            .build()

        // --- 第一步：连接后端 ---
        val backendChannel: Channel = runCatching {
            Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(
                            "backendSsl",
                            clientSslCtx.newHandler(ch.alloc(), tunnel.host, tunnel.port)
                        )
                    }
                })
                .connect(tunnel.host, tunnel.port)
                .awaitChannel()
        }.getOrElse {
            sendAndClose(ctx, HttpResponseStatus.BAD_GATEWAY); return
        }

        // --- 第二步：后端 TLS 握手 ---
        val backendSsl = backendChannel.pipeline().get(SslHandler::class.java)!!

        // 握手期间保留临时错误捕获器，防止握手异常穿透到 Netty tail handler 产生警告日志
        val backendErrCatcher = object : ChannelInboundHandlerAdapter() {
            override fun exceptionCaught(ctx2: ChannelHandlerContext, cause: Throwable) {
                ctx2.close(); ctx.close()
            }
        }
        backendChannel.pipeline().addLast("backendSslErrCatcher", backendErrCatcher)

        // --- 第三步：后端 h2 codec 提前装配（时序关键点）---
        // onSuccess 在 EventLoop 线程同步执行（协程恢复前），确保 Http2FrameCodec 在
        // 服务端首个 SETTINGS 帧到达前就位，消除线程切换导致的竞争窗口。
        var backendProto = ""
        runCatching {
            backendSsl.handshakeFuture().awaitHandshake {
                backendProto = backendSsl.applicationProtocol()
                if (backendProto == ApplicationProtocolNames.HTTP_2) {
                    addHttp2BackendCodec(backendChannel)
                }
            }
        }.getOrElse {
            sendAndClose(ctx, HttpResponseStatus.BAD_GATEWAY); backendChannel.close(); return
        }
        // backendSslErrCatcher 保留在后端管道中，覆盖从此处到 MITM Session 安装完成前的"空窗期"：
        //   - Http1MitmSession.install() 会在安装 BackendResponseHandler 前将其移除（保证正确的异常上报）
        //   - Http2MitmSession 不移除它，将其作为 h2 后端连接的永久保护

        // --- 第四步：发送 200 + 前端管道清理 + 注入 SSL（全部在 awaitWrite.onSuccess 内原子执行）---
        // 原始代码在同一 ChannelFutureListener 回调（EventLoop 线程）中完成这些操作，保证原子性。
        // 协程化后若从 IO 线程逐条提交，EventLoop 会在任务间处理其他事件（如客户端 RST），
        // 导致前端管道空窗期出现 "Connection reset" 穿透到 tail 的警告。
        // 将所有操作放入 awaitWrite.onSuccess（EventLoop 线程）可恢复原子语义，消除空窗期。
        val frontendSsl = CertificateAuthority.getSslContextFor(tunnel.host).newHandler(ctx.channel().alloc())
        val frontendErrCatcher = object : ChannelInboundHandlerAdapter() {
            override fun exceptionCaught(ctx2: ChannelHandlerContext, cause: Throwable) {
                backendChannel.close(); ctx2.close()
            }
        }

        runCatching {
            ctx.writeAndFlush(
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus(200, "Connection Established"),
                    Unpooled.EMPTY_BUFFER
                )
            ).awaitWrite {
                // EventLoop 线程——三次 remove + addFirst + addLast 原子完成，管道无空窗期
                runCatching { ctx.pipeline().remove("httpServerCodec") }
                runCatching { ctx.pipeline().remove("httpAggregator") }
                runCatching { ctx.pipeline().remove(this@ProxyFrontendHandler) }
                check(ctx.channel().isActive) { "channel became inactive after 200 OK" }
                ctx.pipeline().addFirst("frontendSsl", frontendSsl)
                ctx.pipeline().addLast("sslHandshakeErrCatcher", frontendErrCatcher)
            }
        }.getOrElse { backendChannel.close(); return }

        // --- 第五步：前端 TLS 握手 + MITM 管道安装（全部在 awaitHandshake.onSuccess 内原子执行）---
        // 与后端 h2 codec 修复同理：客户端在 TLS 握手完成后立刻发送 h2 preface 或 h1 请求，
        // 若 MITM 管道在 coroutine 恢复后的 IO 线程上安装，则这些首包会在管道就位前被 EventLoop 处理并丢失。
        // 将安装逻辑放入 onSuccess（EventLoop 线程）可确保 MITM 管道在客户端首包到达前已就位。
        runCatching {
            frontendSsl.handshakeFuture().awaitHandshake {
                // EventLoop 线程——移除临时捕获器并安装 MITM，原子完成
                runCatching { ctx.pipeline().remove("sslHandshakeErrCatcher") }
                when (ProtocolDetector.detectMitm(frontendSsl.applicationProtocol(), backendProto)) {
                    is MitmProtocol.Http1 ->
                        Http1MitmSession(tunnel.host, ctx.channel(), backendChannel, onPacketCaptured, idCounter).install()
                    is MitmProtocol.Http2 ->
                        Http2MitmSession(tunnel.host, ctx.channel(), backendChannel, onPacketCaptured, idCounter).install()
                }
            }
        }.getOrElse { backendChannel.close(); return }
    }

    // -------------------------------------------------------------------------
    // 普通 HTTP：直连后端捕获请求/响应
    // -------------------------------------------------------------------------

    private fun handlePlainHttp(
        ctx: ChannelHandlerContext,
        req: FullHttpRequest,
        request: InboundRequest.PlainHttp
    ) {
        if (request.host.isEmpty()) { ctx.close(); return }

        val id = idCounter.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        val startNano = System.nanoTime()

        val requestHeaders = req.headers().entries().associate { it.key to it.value }
        val requestBodyBytes = req.content().let { buf ->
            if (buf.readableBytes() > 0)
                ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
            else null
        }

        // SimpleChannelInboundHandler 在 channelRead0 返回后自动释放 req（content.refCnt → 0）。
        // Bootstrap.connect().addListener 异步触发，届时 req 已被释放。
        // 必须在此（仍在 channelRead0 调用栈内）提前提取所有 listener 内需要的 req 数据：
        //   - methodName / protocolVersion / reqHeadersForForwarding：非引用计数对象，安全持有引用
        //   - contentCopy：ByteBuf 引用计数，必须在此 copy()；连接失败时需手动 release() 防泄漏
        val methodName = req.method().name()
        val protocolVersion = req.protocolVersion()
        val reqHeadersForForwarding = req.headers()
        val contentCopy = req.content().copy()

        Bootstrap()
            .group(ctx.channel().eventLoop())
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(HttpClientCodec())
                    ch.pipeline().addLast(HttpObjectAggregator(MAX_CONTENT_LENGTH))
                }
            })
            .connect(request.host, request.port)
            .addListener(ChannelFutureListener { connectFuture ->
                if (!connectFuture.isSuccess) {
                    contentCopy.release()  // 连接失败，释放副本防止内存泄漏
                    val errorPacket = if (request.isGrpc) {
                        CapturedPacket.Grpc(
                            id = id, timestamp = timestamp, method = methodName,
                            scheme = "http", host = request.host, path = request.path,
                            requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                            error = "连接失败: ${connectFuture.cause()?.message}"
                        )
                    } else {
                        CapturedPacket.Http(
                            id = id, timestamp = timestamp, method = methodName,
                            scheme = "http", host = request.host, path = request.path,
                            requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                            error = "连接失败: ${connectFuture.cause()?.message}"
                        )
                    }
                    onPacketCaptured(errorPacket)
                    sendAndClose(ctx, HttpResponseStatus.BAD_GATEWAY)
                    return@ChannelFutureListener
                }

                val backendChannel = connectFuture.channel()

                backendChannel.pipeline().addLast(object : SimpleChannelInboundHandler<FullHttpResponse>() {
                    override fun channelRead0(ctx2: ChannelHandlerContext, resp: FullHttpResponse) {
                        val duration = (System.nanoTime() - startNano) / 1_000_000
                        val responseHeaders = resp.headers().entries().associate { it.key to it.value }
                        val responseBodyBytes = resp.content().let { buf ->
                            if (buf.readableBytes() > 0)
                                ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
                            else null
                        }
                        val packet = if (request.isGrpc) {
                            CapturedPacket.Grpc(
                                id = id, timestamp = timestamp, method = methodName,
                                scheme = "http", host = request.host, path = request.path,
                                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                                statusCode = resp.status().code(),
                                responseHeaders = responseHeaders, responseBody = responseBodyBytes,
                                durationMs = duration
                            )
                        } else {
                            CapturedPacket.Http(
                                id = id, timestamp = timestamp, method = methodName,
                                scheme = "http", host = request.host, path = request.path,
                                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                                statusCode = resp.status().code(),
                                responseHeaders = responseHeaders, responseBody = responseBodyBytes,
                                durationMs = duration
                            )
                        }
                        onPacketCaptured(packet)
                        ctx.writeAndFlush(resp.retain())
                        backendChannel.close()
                    }

                    override fun exceptionCaught(ctx2: ChannelHandlerContext, cause: Throwable) {
                        val packet = if (request.isGrpc) {
                            CapturedPacket.Grpc(
                                id = id, timestamp = timestamp, method = methodName,
                                scheme = "http", host = request.host, path = request.path,
                                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                                error = cause.message
                            )
                        } else {
                            CapturedPacket.Http(
                                id = id, timestamp = timestamp, method = methodName,
                                scheme = "http", host = request.host, path = request.path,
                                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                                error = cause.message
                            )
                        }
                        onPacketCaptured(packet)
                        ctx2.close()
                        ctx.close()
                    }
                })

                // contentCopy 所有权转移给 newReq，由 Netty 在写入完成后负责释放
                val newReq = DefaultFullHttpRequest(protocolVersion, req.method(), request.path, contentCopy)
                newReq.headers().setAll(reqHeadersForForwarding)
                newReq.headers().set(HttpHeaderNames.HOST, request.host)
                newReq.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                backendChannel.writeAndFlush(newReq)
            })
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }

    private fun sendAndClose(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
        ctx.writeAndFlush(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER))
            .addListener(ChannelFutureListener.CLOSE)
    }
}
