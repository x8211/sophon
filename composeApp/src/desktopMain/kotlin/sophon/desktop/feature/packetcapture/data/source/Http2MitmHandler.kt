package sophon.desktop.feature.packetcapture.data.source

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap
import io.netty.util.ReferenceCountUtil
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * **第一阶段**：在后端 TLS 握手完成后立即调用，将 HTTP/2 客户端 codec 加入后端管道。
 *
 * 必须在后端握手完成后**立即**调用，而不是等到前端握手完成——真实 gRPC 服务端在 TLS 完成后
 * 会马上发出初始 SETTINGS 帧，若 codec 此时不在 pipeline 中，该帧将被丢弃。
 * 等到前端握手（可能有延迟）结束后再添加，codec 看到的第一帧将是 SETTINGS ACK 而非 SETTINGS，
 * 导致 `Http2Exception: First received frame was not SETTINGS`。
 */
internal fun addHttp2BackendCodec(backendChannel: Channel) {
    backendChannel.pipeline().addLast(
        "http2CodecBackend",
        Http2FrameCodecBuilder.forClient()
            .initialSettings(Http2Settings.defaultSettings())
            .build()
    )
    backendChannel.pipeline().addLast(
        "http2MuxBackend",
        Http2MultiplexHandler(object : ChannelInboundHandlerAdapter() {
            // gRPC 不使用 server push，静默丢弃
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) { ReferenceCountUtil.release(msg) }
        })
    )
}

/**
 * **第二阶段**：在前端 TLS 握手完成后调用，将 HTTP/2 服务端 codec 加入前端管道。
 *
 * 此时后端 HTTP/2 codec 已由 [addHttp2BackendCodec] 提前就位，无需重复添加。
 * 每条 HTTP/2 流独立配对：前端流 → [Http2FrontendStreamHandler] → 后端流 → [Http2BackendStreamHandler]。
 * 响应帧**实时转发**（支持 gRPC server-streaming），并在 END_STREAM 时触发抓包回调。
 */
internal fun addHttp2FrontendPipeline(
    frontendChannel: Channel,
    backendChannel: Channel,
    host: String,
    onPacketCaptured: (CapturedPacket) -> Unit,
    idCounter: AtomicLong
) {
    frontendChannel.pipeline().addLast(
        "http2CodecFrontend",
        Http2FrameCodecBuilder.forServer()
            .initialSettings(Http2Settings.defaultSettings())
            .build()
    )
    frontendChannel.pipeline().addLast(
        "http2MuxFrontend",
        Http2MultiplexHandler(object : ChannelInitializer<Channel>() {
            override fun initChannel(ch: Channel) {
                // 每条新入站流创建独立的 handler 实例，保证流级状态隔离
                ch.pipeline().addLast(
                    Http2FrontendStreamHandler(backendChannel, host, onPacketCaptured, idCounter)
                )
            }
        })
    )
}

/**
 * 处理来自客户端的单条 HTTP/2 入站流。
 *
 * 策略：缓冲请求的全部帧直至 END_STREAM，随后在后端连接上创建对应出站流并转发请求。
 * 这对 gRPC 一元调用（unary）和服务端流式调用（server-streaming）均适用，
 * 对于客户端流式 / 双向流式调用则会在 END_STREAM 前阻塞，属当前实现的已知限制。
 */
private class Http2FrontendStreamHandler(
    private val backendParentChannel: Channel,
    private val host: String,
    private val onPacketCaptured: (CapturedPacket) -> Unit,
    private val idCounter: AtomicLong
) : ChannelInboundHandlerAdapter() {

    private var requestHeaders: Http2Headers? = null
    private val requestDataBuf = ByteArrayOutputStream()
    private var committed = false

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            when (msg) {
                is Http2HeadersFrame -> {
                    requestHeaders = msg.headers().deepCopy()
                    if (msg.isEndStream) dispatchToBackend(ctx)
                }
                is Http2DataFrame -> {
                    val buf = msg.content()
                    val bytes = ByteArray(buf.readableBytes())
                    buf.getBytes(buf.readerIndex(), bytes)
                    requestDataBuf.write(bytes)
                    if (msg.isEndStream) dispatchToBackend(ctx)
                }
            }
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    private fun dispatchToBackend(ctx: ChannelHandlerContext) {
        if (committed) return
        committed = true
        val headers = requestHeaders ?: return
        val requestBodyBytes = requestDataBuf.toByteArray().takeIf { it.isNotEmpty() }
        val packetId = idCounter.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        val startNano = System.nanoTime()
        val frontendStream = ctx.channel()

        Http2StreamChannelBootstrap(backendParentChannel)
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline().addLast(
                        Http2BackendStreamHandler(
                            frontendStream = frontendStream,
                            packetId = packetId,
                            timestamp = timestamp,
                            startNano = startNano,
                            host = host,
                            requestHeaders = headers,
                            requestBodyBytes = requestBodyBytes,
                            onPacketCaptured = onPacketCaptured
                        )
                    )
                }
            })
            .open()
            .addListener { openFuture ->
                if (!openFuture.isSuccess) {
                    frontendStream.close()
                    return@addListener
                }
                val backendStream = openFuture.now as Http2StreamChannel

                // 转发请求头（:authority 修正为真实目标 host）
                val outHeaders = headers.deepCopy().apply { authority(host) }
                val hasBody = requestBodyBytes != null
                backendStream.write(DefaultHttp2HeadersFrame(outHeaders, !hasBody))

                // 转发请求体
                if (hasBody) {
                    val bodyBuf = backendStream.alloc()
                        .buffer(requestBodyBytes.size)
                        .writeBytes(requestBodyBytes)
                    backendStream.write(DefaultHttp2DataFrame(bodyBuf, true))
                }
                backendStream.flush()
            }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }
}

/**
 * 处理后端真实服务器对单条 HTTP/2 流的响应。
 *
 * 每个响应帧**立即转发**到对应的前端流，同时在本地累积一份副本；
 * 收到最终 END_STREAM（可能来自 DATA 帧或 trailers HEADERS 帧）后触发 [CapturedPacket] 回调。
 */
private class Http2BackendStreamHandler(
    private val frontendStream: Channel,
    private val packetId: Long,
    private val timestamp: Long,
    private val startNano: Long,
    private val host: String,
    private val requestHeaders: Http2Headers,
    private val requestBodyBytes: ByteArray?,
    private val onPacketCaptured: (CapturedPacket) -> Unit
) : ChannelInboundHandlerAdapter() {

    private var firstStatusCode: Int = 200
    private var firstStatusSet = false
    private val respHeadersAccumulator = mutableListOf<Pair<String, String>>()
    private val respDataBuf = ByteArrayOutputStream()
    private var capturedFired = false

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            when (msg) {
                is Http2HeadersFrame -> {
                    if (!firstStatusSet) {
                        firstStatusCode = msg.headers().status()?.toString()?.toIntOrNull() ?: 200
                        firstStatusSet = true
                    }
                    msg.headers().forEach { e ->
                        respHeadersAccumulator.add(e.key.toString() to e.value.toString())
                    }
                    // 实时转发给前端流（支持 trailers）
                    frontendStream.writeAndFlush(
                        DefaultHttp2HeadersFrame(msg.headers().deepCopy(), msg.isEndStream)
                    )
                    if (msg.isEndStream) fireCapture()
                }
                is Http2DataFrame -> {
                    val buf = msg.content()
                    val bytes = ByteArray(buf.readableBytes())
                    buf.getBytes(buf.readerIndex(), bytes)
                    respDataBuf.write(bytes)
                    // 实时转发（retain 内容，释放帧包装）
                    frontendStream.writeAndFlush(
                        DefaultHttp2DataFrame(msg.content().retain(), msg.isEndStream)
                    )
                    if (msg.isEndStream) fireCapture()
                }
            }
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    private fun fireCapture() {
        if (capturedFired) return
        capturedFired = true

        val duration = (System.nanoTime() - startNano) / 1_000_000
        val path = requestHeaders.path()?.toString() ?: "/"
        val method = requestHeaders.method()?.toString() ?: "POST"
        val scheme = requestHeaders.scheme()?.toString() ?: "https"
        val reqHeadersMap = requestHeaders.toFlatMap()
        val respHeadersMap = respHeadersAccumulator.toMap()
        val responseBodyBytes = respDataBuf.toByteArray().takeIf { it.isNotEmpty() }
        val isGrpc = GrpcDetector.isGrpc(reqHeadersMap)

        val packet = if (isGrpc) {
            CapturedPacket.Grpc(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                statusCode = firstStatusCode,
                responseHeaders = respHeadersMap, responseBody = responseBodyBytes,
                durationMs = duration
            )
        } else {
            CapturedPacket.Http(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                statusCode = firstStatusCode,
                responseHeaders = respHeadersMap, responseBody = responseBodyBytes,
                durationMs = duration
            )
        }
        onPacketCaptured(packet)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
        frontendStream.close()
    }
}

/** 深拷贝 Http2Headers，避免跨线程持有同一可变实例。 */
private fun Http2Headers.deepCopy(): DefaultHttp2Headers =
    DefaultHttp2Headers().also { copy -> forEach { e -> copy.add(e.key, e.value) } }

/** 将 Http2Headers 展平为 String→String Map（相同 key 以最后一个值为准）。 */
private fun Http2Headers.toFlatMap(): Map<String, String> =
    map { e -> e.key.toString() to e.value.toString() }.toMap()
