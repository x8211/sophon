package sophon.desktop.feature.packetcapture.data.source.mitm

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import sophon.desktop.feature.packetcapture.data.source.grpc.GrpcDetector
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * HTTPS MITM 会话中尚未收到响应的请求快照，用于响应到达时还原完整的请求上下文。
 * [isGrpc] 标记该请求是否已被识别为 gRPC，由 [HttpsMitmHandler] 在入队时赋值，
 * 供 [BackendResponseHandler] 组装 [CapturedPacket] 时透传。
 */
internal data class PendingRequest(
    val id: Long,
    val timestamp: Long,
    val startNano: Long,
    val method: String,
    val path: String,
    val requestHeaders: Map<String, String>,
    val requestBody: ByteArray?,
    val isGrpc: Boolean = false
)

/**
 * HTTPS MITM 管道中的请求侧处理器，拦截来自客户端的解密后 HTTP 请求，
 * 将请求元数据压入 [pendingRequests] 队列后转发至后端，维持请求的 FIFO 顺序。
 */
internal class HttpsMitmHandler(
    private val host: String,
    private val backendChannel: Channel,
    internal val pendingRequests: ArrayDeque<PendingRequest>,
    private val onPacketCaptured: (CapturedPacket) -> Unit,
    private val idCounter: AtomicLong
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        val id = idCounter.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        val startNano = System.nanoTime()

        val requestHeaders = req.headers().entries().associate { it.key to it.value }
        val requestBodyBytes = req.content().let { buf ->
            if (buf.readableBytes() > 0)
                ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
            else null
        }

        pendingRequests.addLast(
            PendingRequest(
                id = id, timestamp = timestamp, startNano = startNano,
                method = req.method().name(), path = req.uri(),
                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                isGrpc = GrpcDetector.isGrpc(requestHeaders)
            )
        )

        val newReq = DefaultFullHttpRequest(
            req.protocolVersion(), req.method(), req.uri(),
            req.content().copy()
        )
        newReq.headers().setAll(req.headers())
        newReq.headers().set(HttpHeaderNames.HOST, host)

        backendChannel.writeAndFlush(newReq)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        backendChannel.close()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        while (pendingRequests.isNotEmpty()) {
            val pending = pendingRequests.pollFirst() ?: break
            val packet = if (pending.isGrpc) {
                CapturedPacket.Grpc(
                    id = pending.id, timestamp = pending.timestamp,
                    method = pending.method, scheme = "https", host = host, path = pending.path,
                    requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
                    error = cause.message
                )
            } else {
                CapturedPacket.Http(
                    id = pending.id, timestamp = pending.timestamp,
                    method = pending.method, scheme = "https", host = host, path = pending.path,
                    requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
                    error = cause.message
                )
            }
            onPacketCaptured(packet)
        }
        ctx.close()
        backendChannel.close()
    }
}
