package sophon.desktop.feature.packetcapture.data.source.protocol

/**
 * 代表客户端向代理发起的初始请求类型（第一阶段协议识别结果）。
 *
 * 由 [ProtocolDetector.detect] 从 FullHttpRequest 解析得到，
 * 是 [ProxyFrontendHandler] 进行协议分发的依据。
 */
internal sealed interface InboundRequest {

    /**
     * 普通 HTTP 明文代理请求。
     *
     * @param host    目标主机名
     * @param port    目标端口（默认 80）
     * @param path    请求路径（含 query string）
     * @param isGrpc  是否识别为 gRPC 请求（基于 Content-Type 头）
     */
    data class PlainHttp(
        val host: String,
        val port: Int,
        val path: String,
        val isGrpc: Boolean
    ) : InboundRequest

    /**
     * HTTPS CONNECT 隧道请求，尚未进行 TLS 握手。
     *
     * @param host  目标主机名
     * @param port  目标端口（默认 443）
     */
    data class ConnectTunnel(
        val host: String,
        val port: Int
    ) : InboundRequest
}
