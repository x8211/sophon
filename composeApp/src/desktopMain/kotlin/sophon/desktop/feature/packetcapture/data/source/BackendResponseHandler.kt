package sophon.desktop.feature.packetcapture.data.source

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.ArrayDeque

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

        onPacketCaptured(
            CapturedPacket(
                id = pending.id, timestamp = pending.timestamp,
                method = pending.method, scheme = "https", host = host, path = pending.path,
                requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
                statusCode = resp.status().code(),
                responseHeaders = responseHeaders, responseBody = responseBodyBytes,
                durationMs = duration
            )
        )

        frontendChannel.writeAndFlush(resp.retain())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        frontendChannel.close()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        while (pendingRequests.isNotEmpty()) {
            val pending = pendingRequests.pollFirst() ?: break
            onPacketCaptured(
                CapturedPacket(
                    id = pending.id, timestamp = pending.timestamp,
                    method = pending.method, scheme = "https", host = host, path = pending.path,
                    requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
                    error = cause.message
                )
            )
        }
        ctx.close()
        frontendChannel.close()
    }
}
