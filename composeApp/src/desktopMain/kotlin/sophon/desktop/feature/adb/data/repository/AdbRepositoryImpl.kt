package sophon.desktop.feature.adb.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sophon.desktop.feature.adb.data.source.AdbDataSource
import sophon.desktop.feature.adb.model.AdbState
import sophon.desktop.feature.adb.model.AdbStatus
import java.io.File

class AdbRepositoryImpl(
    private val dataSource: AdbDataSource = AdbDataSource()
) : AdbRepository {

    private val _adbState = MutableStateFlow(AdbState())

    override fun getAdbState(): StateFlow<AdbState> = _adbState.asStateFlow()

    override suspend fun updateAdbPath(path: String) {
        _adbState.update { it.copy(adbToolPath = path) }
    }

    override suspend fun selectDevice(deviceName: String) {
        _adbState.update {
            it.copy(
                status = if (deviceName.isNotBlank()) AdbStatus.Success
                else AdbStatus.Fail("未连接设备或设备已断开"),
                selectedDevice = deviceName
            )
        }
    }

    override suspend fun refreshDevices() {
        val adbPath = _adbState.value.adbToolPath
        val devices = dataSource.fetchConnectedDevices(adbPath)
        _adbState.update { current ->
            val newSelected = when {
                current.selectedDevice.isNotBlank() && devices.contains(current.selectedDevice) -> current.selectedDevice
                else -> devices.firstOrNull() ?: ""
            }
            current.copy(
                connectingDevices = devices,
                selectedDevice = newSelected,
                status = if (newSelected.isNotBlank()) AdbStatus.Success else current.status
            )
        }
    }

    override suspend fun autoFindAdbTool(): String {
        val adbPath = dataSource.resolveBuiltInAdbPath()
        val adbFile = File(adbPath)
        if (adbFile.exists() && !adbFile.canExecute()) {
            adbFile.setExecutable(true)
        }
        return adbPath
    }
}
