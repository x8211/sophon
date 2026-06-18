package sophon.desktop.feature.packetcapture.data.source

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import sophon.desktop.feature.packetcapture.data.source.mitm.ProxyFrontendHandler
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import sophon.desktop.feature.packetcapture.model.ThrottleConfig
import java.util.concurrent.atomic.AtomicLong

private const val MAX_CONTENT_LENGTH = 10 * 1024 * 1024

/** 限速统计窗口：每 500ms 重新计算一次速率，响应更及时。 */
private const val THROTTLE_CHECK_INTERVAL_MS = 500L

/**
 * 最大允许延迟时间：15s（Netty 推荐默认值）。
 * 限速 handler 仅在 TLS 握手完成、MITM 管道安装后插入（作用于解密后的明文层），
 * 因此握手阶段不受此参数影响，15s 可保证大文件下载全程限速生效。
 */
private const val THROTTLE_MAX_DELAY_MS = 15_000L

/**
 * 基于 Netty 的本地 MITM 代理数据源，封装 NIO 事件循环组的启动与停止。
 * 每个进入连接的处理委托给 [ProxyFrontendHandler]，捕获到的数据包通过 [onPacketCaptured] 回调上报。
 * 请求体大小上限为 10 MB。
 *
 * 持有 [scope] 作为所有 CONNECT 隧道协程的调度上下文；[SupervisorJob] 保证单条连接的
 * 协程异常不会取消整个服务器的其他连接；[stop] 时统一 cancel 以终止所有进行中的握手协程。
 *
 * ## 限速架构（两个独立 handler，各负责一段，避免同一字节被计数两次）
 *
 * ```
 * App ──[uploadTrafficShapingHandler: readLimit=uploadBps]──> 代理
 *                                                              ──> 服务器（不限速，写限速=0）
 *
 * 服务器 ──[downloadTrafficShapingHandler: readLimit=downloadBps]──> 代理
 *                                                                    ──> App（不限速，写限速=0）
 * ```
 *
 * - [uploadTrafficShapingHandler]：插入**前端** channel（代理 ↔ App），仅设 readLimit 限制 App→代理上传；
 *   writeLimit=0（代理→App 下行不限速，因为下行已在后端侧控制节奏）。
 * - [downloadTrafficShapingHandler]：插入**后端** channel（代理 ↔ 服务器），仅设 readLimit 限制服务器→代理下载；
 *   后端收到每个 chunk 后立即流式转发给 App，App 的下载节奏与代理收包节奏一致，durationMs 也随之真实反映限速耗时。
 *
 * 两个 handler 均在 TLS 握手完成后插入（作用于解密后的明文流），限速在运行时可通过 [updateThrottle] 动态调整。
 */
class MitmProxyDataSource(private val onPacketCaptured: (CapturedPacket) -> Unit) {

    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null
    private var serverChannel: Channel? = null
    private val idCounter = AtomicLong(0)
    private var scope: CoroutineScope? = null

    /**
     * 前端限速 handler（App ↔ 代理），[GlobalTrafficShapingHandler] 为 @Sharable，可被所有前端 channel 共用。
     * readLimit = uploadBps（限制 App→代理上传速度），writeLimit = 0（代理→App 下行不单独限速）。
     * 在 TLS 握手完成后由 [ProxyFrontendHandler] 动态插入前端 pipeline。
     */
    private var uploadTrafficShapingHandler: GlobalTrafficShapingHandler? = null

    /**
     * 后端限速 handler（代理 ↔ 服务器），[GlobalTrafficShapingHandler] 为 @Sharable，可被所有后端 channel 共用。
     * readLimit = downloadBps（限制服务器→代理下载速度），writeLimit = 0（代理→服务器上行不单独限速）。
     * 在后端 TLS 握手完成后由 [ProxyFrontendHandler] 和 [sophon.desktop.feature.packetcapture.data.source.mitm.BackendChannelManager]
     * 动态插入后端 pipeline。
     */
    private var downloadTrafficShapingHandler: GlobalTrafficShapingHandler? = null

    /** 供 [ProxyFrontendHandler] 读取并注入到**前端** channel 的上传限速 handler。 */
    val currentUploadTrafficShapingHandler: GlobalTrafficShapingHandler? get() = uploadTrafficShapingHandler

    /** 供 [ProxyFrontendHandler] 和 [sophon.desktop.feature.packetcapture.data.source.mitm.BackendChannelManager] 读取并注入到**后端** channel 的下载限速 handler。 */
    val currentDownloadTrafficShapingHandler: GlobalTrafficShapingHandler? get() = downloadTrafficShapingHandler

    /** 记录最新的限速配置，用于服务器重启时恢复。 */
    private var pendingThrottleConfig: ThrottleConfig = ThrottleConfig()

    fun start(port: Int): Result<Unit> {
        return runCatching {
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            bossGroup = NioEventLoopGroup(1)
            val worker = NioEventLoopGroup()
            workerGroup = worker

            val config = pendingThrottleConfig
            // 前端 handler：只限上传（readLimit），写限速=0
            uploadTrafficShapingHandler = GlobalTrafficShapingHandler(
                worker,
                0L,
                config.effectiveUploadBps,
                THROTTLE_CHECK_INTERVAL_MS,
                THROTTLE_MAX_DELAY_MS,
            )
            // 后端 handler：只限下载（readLimit），写限速=0
            downloadTrafficShapingHandler = GlobalTrafficShapingHandler(
                worker,
                0L,
                config.effectiveDownloadBps,
                THROTTLE_CHECK_INTERVAL_MS,
                THROTTLE_MAX_DELAY_MS,
            )

            val bootstrap = ServerBootstrap()
                .group(bossGroup, worker)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        // 不在此处注入 trafficShaping：
                        // CONNECT 路径在 TLS 握手完成、MITM 管道安装后插入（见 ProxyFrontendHandler）；
                        // plain HTTP 路径由 ProxyFrontendHandler.handlePlainHttp 在转发前注入。
                        ch.pipeline().addLast("httpServerCodec", HttpServerCodec())
                        ch.pipeline().addLast("httpAggregator", HttpObjectAggregator(MAX_CONTENT_LENGTH))
                        ch.pipeline().addLast(ProxyFrontendHandler(scope!!, onPacketCaptured, idCounter, this@MitmProxyDataSource))
                    }
                })

            val bindFuture = bootstrap.bind(port).sync()
            if (!bindFuture.isSuccess) {
                throw bindFuture.cause() ?: Exception("绑定端口 $port 失败")
            }
            serverChannel = bindFuture.channel()
        }
    }

    fun stop() {
        serverChannel?.close()?.sync()
        serverChannel = null
        uploadTrafficShapingHandler?.release()
        uploadTrafficShapingHandler = null
        downloadTrafficShapingHandler?.release()
        downloadTrafficShapingHandler = null
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
        bossGroup = null
        workerGroup = null
        scope?.cancel()
        scope = null
        idCounter.set(0)
    }

    /**
     * 动态更新限速配置，立即对所有现存及新建连接生效。
     * 代理未运行时也可调用，配置将在下次 [start] 时应用。
     */
    fun updateThrottle(config: ThrottleConfig) {
        pendingThrottleConfig = config
        // 前端 handler：writeLimit=0（不限下行），readLimit=uploadBps
        uploadTrafficShapingHandler?.configure(0L, config.effectiveUploadBps)
        // 后端 handler：writeLimit=0（不限上行），readLimit=downloadBps
        downloadTrafficShapingHandler?.configure(0L, config.effectiveDownloadBps)
    }

    val isRunning: Boolean get() = serverChannel?.isActive == true
}
