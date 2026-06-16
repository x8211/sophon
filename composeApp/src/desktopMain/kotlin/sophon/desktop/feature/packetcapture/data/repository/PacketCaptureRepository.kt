package sophon.desktop.feature.packetcapture.data.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.packetcapture.model.CapturedPacket

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
}
