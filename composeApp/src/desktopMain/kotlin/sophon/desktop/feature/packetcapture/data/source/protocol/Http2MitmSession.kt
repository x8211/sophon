package sophon.desktop.feature.packetcapture.data.source.protocol

import io.netty.channel.Channel
import sophon.desktop.feature.packetcapture.data.source.addHttp2FrontendPipeline
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import java.util.concurrent.atomic.AtomicLong

/**
 * HTTP/2 HTTPS MITM 会话封装（含 gRPC over h2 支持）。
 *
 * [install] 调用 Http2MitmHandler.kt 中的包级函数，完成前端 HTTP/2 管道装配。
 *
 * **时序约定**：调用本类 [install] 之前，调用方须已在后端 TLS 握手完成后立即调用
 * [addHttp2BackendCodec]，确保后端 codec 在真实服务端首个 SETTINGS 帧到达前就位。
 * 本 Session 仅负责前端管道装配，不重复处理后端 codec。
 */
internal class Http2MitmSession(
    private val host: String,
    private val frontendChannel: Channel,
    private val backendChannel: Channel,
    private val onPacketCaptured: (CapturedPacket) -> Unit,
    private val idCounter: AtomicLong
) {
    fun install() {
        addHttp2FrontendPipeline(frontendChannel, backendChannel, host, onPacketCaptured, idCounter)
    }
}
