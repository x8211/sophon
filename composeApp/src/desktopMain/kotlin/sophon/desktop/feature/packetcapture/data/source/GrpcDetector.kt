package sophon.desktop.feature.packetcapture.data.source


/**
 * gRPC 请求识别与日志工具。
 *
 * 识别规则（基于 gRPC 协议规范）：
 * - `Content-Type` 以 `application/grpc` 开头（涵盖 grpc、grpc+proto、grpc+json、grpc-web 等子类型）
 *
 * 本对象只做识别与日志，不修改请求/响应流，确保流量原样透传（不拦截）。
 */
internal object GrpcDetector {

    /** 根据请求头判断是否为 gRPC 请求。 */
    fun isGrpc(headers: Map<String, String>): Boolean {
        val contentType = headers.entries
            .firstOrNull { it.key.equals("content-type", ignoreCase = true) }
            ?.value ?: return false
        return contentType.startsWith("application/grpc", ignoreCase = true)
    }

}
