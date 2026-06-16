package sophon.desktop.feature.packetcapture.data.source.protocol

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.ssl.ApplicationProtocolNames
import sophon.desktop.feature.packetcapture.data.source.GrpcDetector
import sophon.desktop.feature.packetcapture.data.source.protocol.ProtocolDetector.detect
import sophon.desktop.feature.packetcapture.data.source.protocol.ProtocolDetector.detectMitm
import java.net.URI

/**
 * 协议检测工具，提供两阶段纯函数检测。
 *
 * **第一阶段** [detect]：从初始 HTTP 请求判断请求类型，仅读取请求元数据，无 IO。
 * **第二阶段** [detectMitm]：根据双端 ALPN 协商结果确定 MITM 管道类型。
 *
 * 两个函数均无副作用，可独立于 Netty 服务器进行单元测试。
 */
internal object ProtocolDetector {

    /**
     * 从初始 HTTP 请求判断入站请求类型（第一阶段）。
     *
     * - `CONNECT` 方法 → [InboundRequest.ConnectTunnel]（host:port 解析自 URI）
     * - 其他方法 → [InboundRequest.PlainHttp]（含 host/port/path 及 gRPC 标记）
     *
     * URI 解析失败时返回 [InboundRequest.PlainHttp] 且 host 为空字符串，
     * 调用方应检测并关闭连接。
     */
    fun detect(request: FullHttpRequest): InboundRequest {
        if (request.method() == HttpMethod.CONNECT) {
            val parts = request.uri().split(":")
            val host = parts[0]
            val port = parts.getOrNull(1)?.toIntOrNull() ?: 443
            return InboundRequest.ConnectTunnel(host, port)
        }

        val uri = try { URI(request.uri()) } catch (_: Exception) {
            return InboundRequest.PlainHttp(host = "", port = 80, path = "/", isGrpc = false)
        }
        val host = uri.host ?: return InboundRequest.PlainHttp(host = "", port = 80, path = "/", isGrpc = false)
        val port = if (uri.port == -1) 80 else uri.port
        val path = buildString {
            append(uri.rawPath.ifEmpty { "/" })
            if (uri.rawQuery != null) append("?${uri.rawQuery}")
        }
        val headers = request.headers().entries().associate { it.key to it.value }
        val isGrpc = GrpcDetector.isGrpc(headers)
        return InboundRequest.PlainHttp(host = host, port = port, path = path, isGrpc = isGrpc)
    }

    /**
     * 根据双端 ALPN 协商结果确定 MITM 管道类型（第二阶段）。
     *
     * 双端均为 `h2` → [MitmProtocol.Http2]（含 gRPC over h2）
     * 其他情况 → [MitmProtocol.Http1]（兜底，含任一端降级至 HTTP/1.1 的情形）
     */
    fun detectMitm(clientAlpn: String, serverAlpn: String): MitmProtocol =
        if (clientAlpn == ApplicationProtocolNames.HTTP_2 && serverAlpn == ApplicationProtocolNames.HTTP_2)
            MitmProtocol.Http2
        else
            MitmProtocol.Http1
}
