package sophon.desktop.feature.packetcapture.data.source.mitm

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.DefaultLastHttpContent
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.LastHttpContent
import io.netty.util.ReferenceCountUtil
import sophon.desktop.feature.packetcapture.data.source.mitm.BackendResponseHandler.Companion.MAX_CAPTURE_BODY_BYTES
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque

/**
 * HTTPS MITM 管道中的响应侧处理器，以**流式**方式接收真实服务器的 HTTP 响应。
 *
 * 不依赖 `HttpObjectAggregator`，因此对任意大小的响应均有效——无论响应体是几 KB 还是几百 MB，
 * 都不会因聚合器溢出而中断前端连接。
 *
 * ## 数据流
 * 1. `HttpResponse`（状态行 + 头部）到达时，立即写到前端（OkHttp 端），同时记录请求元数据。
 * 2. 每个 `HttpContent` 块到达时，立即转发到前端，同时在内存中积累**最多 [MAX_CAPTURE_BODY_BYTES]**
 *    字节用于抓包界面展示；超出限制的部分直接丢弃（只转发，不保留）。
 * 3. `LastHttpContent` 到达时，转发后触发 [CapturedPacket] 回调。
 *
 * ## 连接生命周期
 * - 后端连接关闭（[channelInactive]）时，先上报当前未完成请求的错误包，再关闭前端 channel。
 * - 对已入队但尚未开始的请求，同样补发错误抓包记录。
 */
internal class BackendResponseHandler(
    private val frontendChannel: Channel,
    private val pendingRequests: ArrayDeque<PendingRequest>,
    private val host: String,
    private val onPacketCaptured: (CapturedPacket) -> Unit,
) : ChannelInboundHandlerAdapter() {

    private companion object {
        /** 每个响应最多在内存中积累多少字节用于抓包界面展示。 */
        const val MAX_CAPTURE_BODY_BYTES = 1 * 1024 * 1024  // 1 MB
    }

    private var activePending: PendingRequest? = null
    private var activeStatusCode: Int = 0
    private var activeRespHeaders: Map<String, String> = emptyMap()
    private val activeBodyCapture = ByteArrayOutputStream()
    private var capturingBody = true

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            when (msg) {
                is HttpResponse -> onResponseHeaders(msg)
                is HttpContent  -> onResponseContent(msg)
            }
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    private fun onResponseHeaders(resp: HttpResponse) {
        activePending = pendingRequests.pollFirst() ?: return
        activeStatusCode = resp.status().code()
        activeRespHeaders = resp.headers().entries().associate { it.key to it.value }
        activeBodyCapture.reset()
        capturingBody = true
        // 立即将响应头转发到前端，OkHttp 可同步开始读取响应状态与头部。
        frontendChannel.write(DefaultHttpResponse(resp.protocolVersion(), resp.status(), resp.headers()))
    }

    private fun onResponseContent(content: HttpContent) {
        // 在内存中积累部分响应体用于抓包界面（超出上限后仅转发不保存）
        val buf = content.content()
        if (capturingBody && buf.readableBytes() > 0) {
            val remaining = MAX_CAPTURE_BODY_BYTES - activeBodyCapture.size()
            if (remaining > 0) {
                val len = minOf(buf.readableBytes(), remaining)
                val bytes = ByteArray(len)
                buf.getBytes(buf.readerIndex(), bytes)
                activeBodyCapture.write(bytes)
            } else {
                capturingBody = false
            }
        }
        // 立即将内容块转发到前端（retain 使 ByteBuf 在 finally-release 之后仍有效）
        if (content is LastHttpContent) {
            frontendChannel.writeAndFlush(DefaultLastHttpContent(content.content().retain()))
            fireCapture(error = null)
        } else {
            frontendChannel.writeAndFlush(DefaultHttpContent(content.content().retain()))
        }
    }

    private fun fireCapture(error: String?) {
        val pending = activePending ?: return
        activePending = null
        val duration = (System.nanoTime() - pending.startNano) / 1_000_000
        val responseBodyBytes = activeBodyCapture.toByteArray().takeIf { it.isNotEmpty() }
        onPacketCaptured(buildPacket(pending, duration, responseBodyBytes, error))
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        // 后端连接在响应传输途中关闭：补发带错误的抓包记录
        fireCapture(error = "Connection closed mid-response")
        // 已入队但从未开始的请求：同样补发错误记录
        drainPendingWithError("Connection closed")
        frontendChannel.close()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        val errMsg = cause.message ?: "Backend error"
        fireCapture(error = errMsg)
        drainPendingWithError(errMsg)
        ctx.close()
        frontendChannel.close()
    }

    private fun drainPendingWithError(error: String) {
        while (pendingRequests.isNotEmpty()) {
            val pending = pendingRequests.pollFirst() ?: break
            onPacketCaptured(buildPacket(pending, durationMs = 0, responseBodyBytes = null, error = error))
        }
    }

    private fun buildPacket(
        pending: PendingRequest,
        durationMs: Long,
        responseBodyBytes: ByteArray?,
        error: String?,
    ): CapturedPacket = if (pending.isGrpc) {
        CapturedPacket.Grpc(
            id = pending.id, timestamp = pending.timestamp,
            method = pending.method, scheme = "https", host = host, path = pending.path,
            requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
            statusCode = if (error == null) activeStatusCode else null,
            responseHeaders = activeRespHeaders, responseBody = responseBodyBytes,
            durationMs = durationMs, error = error,
        )
    } else {
        CapturedPacket.Http(
            id = pending.id, timestamp = pending.timestamp,
            method = pending.method, scheme = "https", host = host, path = pending.path,
            requestHeaders = pending.requestHeaders, requestBody = pending.requestBody,
            statusCode = if (error == null) activeStatusCode else null,
            responseHeaders = activeRespHeaders, responseBody = responseBodyBytes,
            durationMs = durationMs, error = error,
        )
    }
}
