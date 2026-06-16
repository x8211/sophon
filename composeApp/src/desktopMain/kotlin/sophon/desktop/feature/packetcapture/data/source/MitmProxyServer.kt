package sophon.desktop.feature.packetcapture.data.source

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.concurrent.atomic.AtomicLong

private const val MAX_CONTENT_LENGTH = 10 * 1024 * 1024

/**
 * 基于 Netty 的本地 MITM 代理服务器，封装 NIO 事件循环组的启动与停止。
 * 每个进入连接的处理委托给 [ProxyFrontendHandler]，捕获到的数据包通过 [onPacketCaptured] 回调上报。
 * 请求体大小上限为 10 MB。
 */
class MitmProxyServer(private val onPacketCaptured: (CapturedPacket) -> Unit) {

    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null
    private var serverChannel: Channel? = null
    private val idCounter = AtomicLong(0)

    fun start(port: Int): Result<Unit> {
        return runCatching {
            bossGroup = NioEventLoopGroup(1)
            workerGroup = NioEventLoopGroup()

            val bootstrap = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("httpServerCodec", HttpServerCodec())
                        ch.pipeline().addLast("httpAggregator", HttpObjectAggregator(MAX_CONTENT_LENGTH))
                        ch.pipeline().addLast(ProxyFrontendHandler(onPacketCaptured, idCounter))
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
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
        bossGroup = null
        workerGroup = null
        idCounter.set(0)
    }

    val isRunning: Boolean get() = serverChannel?.isActive == true
}
