package sophon.desktop.feature.packetcapture.data.source.protocol

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import sophon.desktop.feature.packetcapture.data.source.BackendResponseHandler
import sophon.desktop.feature.packetcapture.data.source.HttpsMitmHandler
import sophon.desktop.feature.packetcapture.data.source.PendingRequest
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

private const val MAX_CONTENT_LENGTH = 10 * 1024 * 1024

/**
 * HTTP/1.1 HTTPS MITM 会话封装。
 *
 * 内聚持有请求/响应配对队列 [pendingRequests]，不再暴露给装配层。
 * [install] 一次调用完成前端（请求侧）和后端（响应侧）两条管道的装配，
 * 确保两侧使用同一个队列实例，维持 FIFO 配对语义。
 *
 * 原 [ProxyFrontendHandler.setupMitmPipeline] 中的 pendingRequests 创建及传递逻辑
 * 迁入本类内部，外部不可见。
 */
internal class Http1MitmSession(
    private val host: String,
    private val frontendChannel: Channel,
    private val backendChannel: Channel,
    private val onPacketCaptured: (CapturedPacket) -> Unit,
    private val idCounter: AtomicLong
) {
    private val pendingRequests = ArrayDeque<PendingRequest>()

    fun install() {
        frontendChannel.pipeline().apply {
            addLast("httpServerCodecMitm", HttpServerCodec())
            addLast("httpAggregatorMitm", HttpObjectAggregator(MAX_CONTENT_LENGTH))
            addLast(HttpsMitmHandler(host, backendChannel, pendingRequests, onPacketCaptured, idCounter))
        }
        backendChannel.pipeline().apply {
            // 在安装 BackendResponseHandler 前移除空窗期保护器：
            // 确保 BackendResponseHandler 成为后端管道中最终的异常处理器，
            // 能够将连接错误正确上报为 CapturedPacket 而非被静默关闭。
            runCatching { remove("backendSslErrCatcher") }
            addLast("httpClientCodec", HttpClientCodec())
            addLast("httpAggregatorBackend", HttpObjectAggregator(MAX_CONTENT_LENGTH))
            addLast(BackendResponseHandler(frontendChannel, pendingRequests, host, onPacketCaptured))
        }
    }
}
