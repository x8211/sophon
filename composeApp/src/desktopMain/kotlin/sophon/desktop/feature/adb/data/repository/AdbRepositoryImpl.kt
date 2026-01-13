package sophon.desktop.feature.adb.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.adb.domain.model.AdbState
import sophon.desktop.feature.adb.domain.model.AdbStatus
import sophon.desktop.feature.adb.domain.repository.AdbRepository
import java.io.File

/**
 * AdbRepository 的实现类，负责具体的 ADB 操作逻辑
 */
class AdbRepositoryImpl() : AdbRepository {

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
        val state = _adbState.value
        val adbPath = state.adbToolPath

        val devices = "$adbPath devices".oneshotShell { result ->
            val pattern = Regex("^([a-zA-Z0-9-]+)\\s+device$", RegexOption.MULTILINE)
            pattern.findAll(result).map { mr -> mr.groupValues[1] }.toList()
        }

        _adbState.update { current ->
            val currentSelected = current.selectedDevice
            val newSelected =
                if (currentSelected.isNotBlank() && devices.contains(currentSelected)) {
                    currentSelected
                } else {
                    devices.firstOrNull() ?: ""
                }

            current.copy(
                connectingDevices = devices,
                selectedDevice = newSelected,
                status = if (newSelected.isNotBlank()) AdbStatus.Success else current.status
            )
        }
    }

    override suspend fun autoFindAdbTool(): String {
        val adbPath = resolveBuiltInAdbPath()
        val adbFile = File(adbPath)
        if (adbFile.exists() && !adbFile.canExecute()) {
            adbFile.setExecutable(true)
        }
        return adbPath
    }

    private fun resolveBuiltInAdbPath(): String {
        // Compose Desktop 打包后的资源目录属性
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            // 打包模式：在资源目录下的 tools/adb
            val deployedAdb = File("/Applications/Sophon.app/Contents/Resources/tools", "adb")
            if (deployedAdb.exists()) {
                return deployedAdb.absolutePath
            }
        }

        // Debug/开发模式：尝试多个可能的路径
        val candidatePaths = listOf(
            "composeApp/src/desktopMain/tools/adb",
            "src/desktopMain/tools/adb",
            "tools/adb"
        )

        return candidatePaths.firstOrNull { File(it).exists() }
            ?: File("composeApp/src/desktopMain/tools/adb").absolutePath
    }
}
