package sophon.desktop.feature.packetcapture.data.source

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.ArrayDeque

/**
 * HTTPS MITM 管道中的响应侧处理器，接收真实服务器的 HTTP 响应，
 * 从 [pendingRequests] 队列头部弹出对应请求，组装完整的 [CapturedPacket] 后触发回调，
 * 并将响应原样转发回客户端。
 */
internal class BackendResponseHandler(
    private val frontendChannel: Channel,
    private val pendingRequests: ArrayDeque<PendingRequest>,
    private val host: String,
    private val onPacketCaptured: (CapturedPacket) -> Unit
) : SimpleChannelInboundHandler<FullHttpResponse>() {

    override fun channelRead0(ctx: ChannelHandlerContext, resp: FullHttpResponse) {
        val pending = pendingRequests.pollFirst() ?: return
        val duration = (System.nanoTime() - pending.startNano) / 1_000_000

        val responseHeaders = resp.headers().entries().associate { it.key to it.value }
        val responseBodyBytes = resp.content().let { buf ->
            if (buf.readableBytes() > 0)
                ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
            else null
        }

        val packet = if (pending.isGrpc) {
            CapturedPacket.Grpc(
                id = pending.id, timestamp = pending.timestamp,
                method = pending.method, scheme = "https", host = host, path = pending.path,
                requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
                statusCode = resp.status().code(),
                responseHeaders = responseHeaders, responseBody = responseBodyBytes,
                durationMs = duration
            )
        } else {
            CapturedPacket.Http(
                id = pending.id, timestamp = pending.timestamp,
                method = pending.method, scheme = "https", host = host, path = pending.path,
                requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
                statusCode = resp.status().code(),
                responseHeaders = responseHeaders, responseBody = responseBodyBytes,
                durationMs = duration
            )
        }
        onPacketCaptured(packet)

        frontendChannel.writeAndFlush(resp.retain())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        frontendChannel.close()
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
        frontendChannel.close()
    }
}
