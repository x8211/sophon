package sophon.desktop.feature.adb.data.repository

import kotlinx.coroutines.flow.StateFlow
import sophon.desktop.feature.adb.model.AdbState

interface AdbRepository {
    fun getAdbState(): StateFlow<AdbState>
    suspend fun updateAdbPath(path: String)
    suspend fun selectDevice(deviceName: String)
    suspend fun refreshDevices()
    suspend fun autoFindAdbTool(): String
}
