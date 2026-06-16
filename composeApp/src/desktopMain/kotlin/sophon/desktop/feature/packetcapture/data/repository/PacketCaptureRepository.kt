package sophon.desktop.feature.packetcapture.data.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.packetcapture.model.CapturedPacket

interface PacketCaptureRepository {
    fun startCapture(port: Int): Flow<CapturedPacket>
    fun stopCapture()
    suspend fun installCaToDevice()
    suspend fun getDeviceProxy(): String
    fun getCaCertPath(): String
}
