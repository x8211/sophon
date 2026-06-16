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
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.net.URI
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

private const val MAX_CONTENT_LENGTH = 10 * 1024 * 1024

class ProxyFrontendHandler(
    private val onPacketCaptured: (CapturedPacket) -> Unit,
    private val idCounter: AtomicLong
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        if (msg.method() == HttpMethod.CONNECT) {
            handleConnect(ctx, msg)
        } else {
            handleHttp(ctx, msg)
        }
    }

    private fun handleConnect(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        val parts = req.uri().split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 443

        val clientSslCtx = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()

        Bootstrap()
            .group(ctx.channel().eventLoop())
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast("backendSsl", clientSslCtx.newHandler(ch.alloc(), host, port))
                }
            })
            .connect(host, port)
            .addListener(ChannelFutureListener { connectFuture ->
                if (!connectFuture.isSuccess) {
                    sendAndClose(ctx, HttpResponseStatus.BAD_GATEWAY)
                    return@ChannelFutureListener
                }

                val backendChannel = connectFuture.channel()
                val backendSslHandler = backendChannel.pipeline().get(SslHandler::class.java)!!

                // 后端 SSL 握手失败时同样需要捕获 exceptionCaught，防止穿透到 tail
                backendChannel.pipeline().addLast("backendSslErrCatcher",
                    object : ChannelInboundHandlerAdapter() {
                        override fun exceptionCaught(ctx2: ChannelHandlerContext, cause: Throwable) {
                            ctx2.close()
                            ctx.close()
                        }
                    }
                )

                backendSslHandler.handshakeFuture().addListener { backendHsFuture ->
                    if (!backendHsFuture.isSuccess) {
                        // 同前端逻辑：保留 backendSslErrCatcher，由它处理随后的 exceptionCaught
                        sendAndClose(ctx, HttpResponseStatus.BAD_GATEWAY)
                        return@addListener
                    }
                    runCatching { backendChannel.pipeline().remove("backendSslErrCatcher") }

                    val frontendPipeline = ctx.pipeline()
                    ctx.writeAndFlush(
                        DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus(200, "Connection Established"),
                            Unpooled.EMPTY_BUFFER
                        )
                    ).addListener(ChannelFutureListener {
                        frontendPipeline.remove("httpServerCodec")
                        frontendPipeline.remove("httpAggregator")
                        frontendPipeline.remove(this@ProxyFrontendHandler)

                        val fakeSslHandler = CertificateAuthority.getSslContextFor(host)
                            .newHandler(ctx.channel().alloc())
                        frontendPipeline.addFirst("frontendSsl", fakeSslHandler)

                        // 握手期间 pipeline 里只有 SslHandler，若握手失败（如设备未安装 CA 证书）
                        // 异常会传到 tail 并打印警告。加一个临时捕获器安静地关闭连接。
                        frontendPipeline.addLast("sslHandshakeErrCatcher",
                            object : ChannelInboundHandlerAdapter() {
                                override fun exceptionCaught(ctx2: ChannelHandlerContext, cause: Throwable) {
                                    backendChannel.close()
                                    ctx2.close()
                                }
                            }
                        )

                        fakeSslHandler.handshakeFuture().addListener { frontendHsFuture ->
                            if (!frontendHsFuture.isSuccess) {
                                // handshake future listener 早于 exceptionCaught 触发。
                                // 若此处移除 sslHandshakeErrCatcher，随后的 exceptionCaught
                                // 将无处捕获，穿透到 pipeline tail 打印警告。
                                // 保留 sslHandshakeErrCatcher，让它拦截那个 exceptionCaught 并关闭连接。
                                backendChannel.close()
                                return@addListener
                            }
                            // 握手成功：移除临时捕获器，建立 MITM 管道
                            runCatching { frontendPipeline.remove("sslHandshakeErrCatcher") }
                            setupMitmPipeline(ctx.channel(), backendChannel, host)
                        }
                    })
                }
            })
    }

    private fun setupMitmPipeline(frontendChannel: Channel, backendChannel: Channel, host: String) {
        val pendingRequests = ArrayDeque<PendingRequest>()

        frontendChannel.pipeline().addLast("httpServerCodecMitm", HttpServerCodec())
        frontendChannel.pipeline().addLast("httpAggregatorMitm", HttpObjectAggregator(MAX_CONTENT_LENGTH))
        frontendChannel.pipeline().addLast(
            "httpsMitmHandler",
            HttpsMitmHandler(host, backendChannel, pendingRequests, onPacketCaptured, idCounter)
        )

        backendChannel.pipeline().addLast("httpClientCodec", HttpClientCodec())
        backendChannel.pipeline().addLast("httpAggregatorBackend", HttpObjectAggregator(MAX_CONTENT_LENGTH))
        backendChannel.pipeline().addLast(
            "backendResponseHandler",
            BackendResponseHandler(frontendChannel, pendingRequests, host, onPacketCaptured)
        )
    }

    private fun handleHttp(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        val id = idCounter.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        val startNano = System.nanoTime()

        val uri = try { URI(req.uri()) } catch (e: Exception) {
            ctx.close(); return
        }
        val host = uri.host ?: run { ctx.close(); return }
        val port = if (uri.port == -1) 80 else uri.port
        val path = buildString {
            append(uri.rawPath.ifEmpty { "/" })
            if (uri.rawQuery != null) append("?${uri.rawQuery}")
        }

        val requestHeaders = req.headers().entries().associate { it.key to it.value }
        val requestBodyBytes = req.content().let { buf ->
            if (buf.readableBytes() > 0)
                ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
            else null
        }

        Bootstrap()
            .group(ctx.channel().eventLoop())
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(HttpClientCodec())
                    ch.pipeline().addLast(HttpObjectAggregator(MAX_CONTENT_LENGTH))
                }
            })
            .connect(host, port)
            .addListener(ChannelFutureListener { connectFuture ->
                if (!connectFuture.isSuccess) {
                    onPacketCaptured(
                        CapturedPacket(
                            id = id, timestamp = timestamp, method = req.method().name(),
                            scheme = "http", host = host, path = path,
                            requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                            error = "连接失败: ${connectFuture.cause()?.message}"
                        )
                    )
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
                        onPacketCaptured(
                            CapturedPacket(
                                id = id, timestamp = timestamp, method = req.method().name(),
                                scheme = "http", host = host, path = path,
                                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                                statusCode = resp.status().code(),
                                responseHeaders = responseHeaders, responseBody = responseBodyBytes,
                                durationMs = duration
                            )
                        )
                        ctx.writeAndFlush(resp.retain())
                        backendChannel.close()
                    }

                    override fun exceptionCaught(ctx2: ChannelHandlerContext, cause: Throwable) {
                        onPacketCaptured(
                            CapturedPacket(
                                id = id, timestamp = timestamp, method = req.method().name(),
                                scheme = "http", host = host, path = path,
                                requestHeaders = requestHeaders, requestBody = requestBodyBytes,
                                error = cause.message
                            )
                        )
                        ctx2.close()
                        ctx.close()
                    }
                })

                val newReq = DefaultFullHttpRequest(
                    req.protocolVersion(), req.method(), path,
                    req.content().copy()
                )
                newReq.headers().setAll(req.headers())
                newReq.headers().set(HttpHeaderNames.HOST, host)
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
