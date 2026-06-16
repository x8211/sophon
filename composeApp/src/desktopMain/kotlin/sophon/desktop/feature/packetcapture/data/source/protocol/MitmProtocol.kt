package sophon.desktop.feature.packetcapture.data.source.protocol

/**
 * 代表经 TLS 握手 + ALPN 协商后确定的 MITM 管道类型（第二阶段协议识别结果）。
 *
 * 由 [ProtocolDetector.detectMitm] 根据双端 ALPN 协商结果得到，
 * 是 [ProxyFrontendHandler.handleConnect] 选择 Session 实现的依据。
 */
internal sealed interface MitmProtocol {

    /** HTTP/1.1 MITM 管道——任一端未协商到 h2 时的兜底类型。 */
    data object Http1 : MitmProtocol

    /** HTTP/2 MITM 管道——双端均协商到 h2 时（含 gRPC over h2）。 */
    data object Http2 : MitmProtocol
}
