package sophon.desktop.feature.packetcapture.data.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import sophon.desktop.feature.packetcapture.model.GrpcMockRule
import sophon.desktop.feature.packetcapture.model.ThrottleConfig

/**
 * 抓包数据层对外接口，定义启动/停止代理服务、查询设备代理及推送 CA 证书的契约。
 * ViewModel 仅依赖此接口，不直接持有具体实现。
 */
interface PacketCaptureRepository {
    fun startCapture(port: Int): Flow<CapturedPacket>
    fun stopCapture()
    suspend fun installCaToDevice()
    suspend fun getDeviceProxy(): String
    fun getCaCertPath(): String
    /** 动态更新限速配置；代理未运行时也可调用，配置将在下次启动时应用。 */
    fun updateThrottle(config: ThrottleConfig)
    /** 更新 gRPC Mock 规则，立即生效（代理运行中也可调用）。 */
    fun updateMockRules(rules: List<GrpcMockRule>, encodedBodies: Map<String, ByteArray>)
}
