package sophon.desktop.feature.adb.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.core.datastore.adbDataStore
import sophon.desktop.feature.adb.domain.model.AdbState
import sophon.desktop.feature.adb.domain.model.AdbStatus
import sophon.desktop.feature.adb.domain.repository.AdbRepository
import java.io.File

/**
 * AdbRepository 的实现类，负责具体的 ADB 操作逻辑
 */
class AdbRepositoryImpl(scope: CoroutineScope) : AdbRepository {

    private val _adbState = MutableStateFlow(AdbState())

    init {
        // 监听 DataStore 变化
        scope.launch {
            adbDataStore.data.collect { data ->
                val adbPath = data.toolPath
                val adbFile = File(adbPath)
                val available = adbFile.isFile && adbFile.name.matches("^adb(\\.exe)?$".toRegex())

                _adbState.update { state ->
                    state.copy(
                        adbToolPath = adbPath,
                        adbToolAvailable = available,
                        adbParentPath = if (available) adbFile.parentFile.absolutePath else null,
                        status = if (available) AdbStatus.Success else AdbStatus.Fail("ADB 工具不可用")
                    )
                }

                // 如果可用，自动刷新一次设备
                if (available) {
                    refreshDevices()
                }
            }
        }
    }

    override fun getAdbState(): StateFlow<AdbState> = _adbState.asStateFlow()

    override suspend fun updateAdbPath(path: String) {
        adbDataStore.updateData { it.copy(toolPath = path) }
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
        val adbFile = File(adbPath)
        val available = adbFile.isFile && adbFile.name.matches("^adb(\\.exe)?$".toRegex())

        if (!available) return

        // 这里的 adb 执行可能需要完整的路径
        val adbCmd = if (state.adbParentPath != null) "${state.adbParentPath}/adb" else "adb"

        val devices = "$adbCmd devices".oneshotShell { result ->
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

    override suspend fun autoFindAdbTool(): String? {
        // ... 原有逻辑保持
        val env = System.getenv()
        val path = env["PATH"] ?: ""
        val home = System.getProperty("user.home")

        val androidSdkPath = "${home}/Library/Android/sdk"
        val platformToolsPath = "$androidSdkPath/platform-tools"
        val newPath =
            "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:$platformToolsPath:$path"

        val shellCommand = """
                export PATH='$newPath'
                whereis adb
            """.trimIndent()

        val adbPath = shellCommand.simpleShell().substringAfter("adb:", "").trim()

        if (adbPath.isNotBlank() && File(adbPath).isFile) {
            return adbPath
        }
        return null
    }
}
