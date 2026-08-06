package sophon.desktop.feature.packetcapture.data.source.mitm

import io.netty.buffer.Unpooled
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
import sophon.desktop.feature.packetcapture.data.source.grpc.GrpcDetector
import sophon.desktop.feature.packetcapture.data.source.grpc.GrpcMockRegistry
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
 *
 * @param backendManager 管理后端连接生命周期，在 GOAWAY 后自动重建连接
 */
internal fun addHttp2FrontendPipeline(
    frontendChannel: Channel,
    backendManager: BackendChannelManager,
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
                    Http2FrontendStreamHandler(backendManager, host, onPacketCaptured, idCounter)
                )
            }
        })
    )
}

/**
 * 处理来自客户端的单条 HTTP/2 入站流。
 *
 * 策略：缓冲请求的全部帧直至 END_STREAM，随后通过 [BackendChannelManager.withChannel] 获取
 * 可用后端连接（必要时自动重建），再开子流转发请求。
 * 这对 gRPC 一元调用（unary）和服务端流式调用（server-streaming）均适用，
 * 对于客户端流式 / 双向流式调用则会在 END_STREAM 前阻塞，属当前实现的已知限制。
 */
private class Http2FrontendStreamHandler(
    private val backendManager: BackendChannelManager,
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

        // 请求发出时立即上报，使其立刻出现在列表中（statusCode=null 显示为「...」）；
        // 响应到达后 Http2BackendStreamHandler 会用相同 id 再次上报完整包，ViewModel 负责替换。
        val reqHeadersMap = headers.toFlatMap()
        val isGrpc = GrpcDetector.isGrpc(reqHeadersMap)
        val pendingPath = headers.path()?.toString() ?: "/"
        val pendingMethod = headers.method()?.toString() ?: "POST"
        val pendingScheme = headers.scheme()?.toString() ?: "https"
        onPacketCaptured(
            if (isGrpc) {
                CapturedPacket.Grpc(
                    id = packetId, timestamp = timestamp,
                    method = pendingMethod, scheme = pendingScheme, host = host, path = pendingPath,
                    requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                )
            } else {
                CapturedPacket.Http(
                    id = packetId, timestamp = timestamp,
                    method = pendingMethod, scheme = pendingScheme, host = host, path = pendingPath,
                    requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                )
            }
        )

        // gRPC Mock 短路：命中规则时直接写 mock 响应，不请求真实后端
        val mockResult = if (isGrpc) GrpcMockRegistry.findMatch(host, pendingPath) else null
        if (mockResult != null) {
            writeMockResponse(
                frontendStream, mockResult,
                packetId, timestamp, startNano,
                pendingMethod, pendingScheme, pendingPath, reqHeadersMap, requestBodyBytes
            )
            return
        }

        backendManager.withChannel(
            eventLoop = ctx.channel().eventLoop(),
            action = { backendParentChannel ->
                openBackendStream(
                    backendParentChannel, frontendStream,
                    packetId, timestamp, startNano, headers, requestBodyBytes
                )
            },
            onError = { cause ->
                // 重建后端连接失败（GOAWAY 且重连不可达），记录错误包后关闭前端流
                fireErrorPacket(packetId, timestamp, startNano, headers, requestBodyBytes, cause.message ?: "Reconnect failed")
                frontendStream.close()
            }
        )
    }

    /**
     * 向前端流写 Mock 响应（HEADERS → DATA → trailers），绕过真实后端。
     * 完成后触发 [onPacketCaptured] 上报带 [CapturedPacket.Grpc.isMocked]=true 的完整抓包记录。
     */
    private fun writeMockResponse(
        frontendStream: Channel,
        mockResult: GrpcMockRegistry.MockResult,
        packetId: Long,
        timestamp: Long,
        startNano: Long,
        method: String,
        scheme: String,
        path: String,
        reqHeadersMap: Map<String, String>,
        requestBodyBytes: ByteArray?
    ) {
        val durationMs = (System.nanoTime() - startNano) / 1_000_000
        val responseHeaders = mapOf(
            ":status" to "200",
            "content-type" to "application/grpc+proto",
        )
        val trailerGrpcStatus = mockResult.grpcStatus.toString()
        val responseAllHeaders = responseHeaders + mapOf(
            "grpc-status" to trailerGrpcStatus,
            "grpc-message" to if (mockResult.grpcStatus == 0) "OK" else "Mock error",
        )

        // 写 HEADERS（:status=200）
        val respHeaders = DefaultHttp2Headers()
            .status("200")
            .add("content-type", "application/grpc+proto")
        frontendStream.write(DefaultHttp2HeadersFrame(respHeaders, false))

        // 写 DATA（带 5 字节 gRPC 帧头的 protobuf bytes）
        val dataBuf = Unpooled.wrappedBuffer(mockResult.encodedBody)
        frontendStream.write(DefaultHttp2DataFrame(dataBuf, false))

        // 写 trailers（grpc-status + END_STREAM）
        val trailers = DefaultHttp2Headers()
            .add("grpc-status", trailerGrpcStatus)
            .add("grpc-message", if (mockResult.grpcStatus == 0) "OK" else "Mock error")
        frontendStream.writeAndFlush(DefaultHttp2HeadersFrame(trailers, true))

        // 上报完整 Mock 抓包记录
        onPacketCaptured(
            CapturedPacket.Grpc(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                statusCode = 200,
                responseHeaders = responseAllHeaders,
                responseBody = mockResult.encodedBody,
                durationMs = durationMs,
                isMocked = true,
            )
        )
    }


    private fun openBackendStream(
        backendParentChannel: Channel,
        frontendStream: Channel,
        packetId: Long,
        timestamp: Long,
        startNano: Long,
        headers: Http2Headers,
        requestBodyBytes: ByteArray?
    ) {
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
                    // open() 失败：通常因为 GOAWAY 在 withChannel 检查后、open() 前的极小窗口内到达。
                    // 记录错误包，前端流关闭使 App 感知到 CANCELLED 并自行重试。
                    val errMsg = openFuture.cause()?.message ?: "Failed to open backend stream"
                    fireErrorPacket(packetId, timestamp, startNano, headers, requestBodyBytes, errMsg)
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
                        .buffer(requestBodyBytes!!.size)
                        .writeBytes(requestBodyBytes)
                    backendStream.write(DefaultHttp2DataFrame(bodyBuf, true))
                }
                backendStream.flush()
            }
    }

    private fun fireErrorPacket(
        packetId: Long,
        timestamp: Long,
        startNano: Long,
        headers: Http2Headers,
        requestBodyBytes: ByteArray?,
        errMsg: String
    ) {
        val reqHeadersMap = headers.toFlatMap()
        val isGrpc = GrpcDetector.isGrpc(reqHeadersMap)
        val duration = (System.nanoTime() - startNano) / 1_000_000
        val path = headers.path()?.toString() ?: "/"
        val method = headers.method()?.toString() ?: "POST"
        val scheme = headers.scheme()?.toString() ?: "https"
        val packet = if (isGrpc) {
            CapturedPacket.Grpc(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                durationMs = duration, error = errMsg
            )
        } else {
            CapturedPacket.Http(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                durationMs = duration, error = errMsg
            )
        }
        onPacketCaptured(packet)
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
 * 若流被 RST_STREAM 取消，[channelInactive] 补发带错误的抓包记录。
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

    /**
     * 触发抓包回调。
     *
     * [error] 非 null 表示流因异常终止（RST_STREAM / 连接断开）而非正常 END_STREAM 结束。
     * 此时若服务端尚未发送过任何状态码（[firstStatusSet] = false），则 statusCode 保持 null，
     * 避免将默认值 200 误报为正常响应。
     */
    private fun fireCapture(error: String? = null) {
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
        // 仅当服务端确实发送过 status 时才使用，避免 RST 场景误填默认值 200
        val statusCode = if (firstStatusSet) firstStatusCode else null

        val packet = if (isGrpc) {
            CapturedPacket.Grpc(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                statusCode = statusCode,
                responseHeaders = respHeadersMap, responseBody = responseBodyBytes,
                durationMs = duration, error = error
            )
        } else {
            CapturedPacket.Http(
                id = packetId, timestamp = timestamp,
                method = method, scheme = scheme, host = host, path = path,
                requestHeaders = reqHeadersMap, requestBody = requestBodyBytes,
                statusCode = statusCode,
                responseHeaders = respHeadersMap, responseBody = responseBodyBytes,
                durationMs = duration, error = error
            )
        }
        onPacketCaptured(packet)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        // 流被 RST_STREAM 取消（非正常 END_STREAM 终止），补发带错误的抓包记录并关闭前端流。
        // capturedFired 守卫确保正常结束后 channelInactive 触发时不会重复上报。
        if (!capturedFired) {
            fireCapture(error = "CANCELLED (RST_STREAM)")
            frontendStream.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        fireCapture(error = cause.message ?: "Stream error")
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
