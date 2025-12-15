package sophon.desktop.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.datastore.adbDataStore
import java.io.File

object Context {

    private val scope = MainScope()
    private val _tick = MutableStateFlow(-1L) //定时器
    private val _stream = MutableStateFlow(State())
    val stream = _stream.asStateFlow()

    var adbParentPath: String? = null
    private val selectedDevice get() = _stream.value.selectedDevice

    init {
        scope.launch {
            while (true) {
                _tick.value = System.currentTimeMillis()
                delay(3000)
            }
        }

        scope.launch {
            combine(adbDataStore.data, _tick) { it, _ -> it }
                .collect {
                    val adbFile = File(it.toolPath)
                    val adbAvailable =
                        adbFile.isFile && adbFile.name.matches("^adb(\\.exe)?$".toRegex())
                    if (!adbAvailable) autoFindAdbTool()
                    else {
                        _stream.update { state ->
                            state.copy(
                                status = State.Status.Success,
                                adbToolPath = it.toolPath,
                                adbToolAvailable = true
                            )
                        }
                        adbParentPath = adbFile.parentFile.absolutePath

                        val devices = "adb devices".oneshotShell { result ->
                            val pattern =
                                Regex("^([a-zA-Z0-9-]+)\\s+device$", RegexOption.MULTILINE)
                            pattern.findAll(result).map { mr -> mr.groupValues[1] }.toList()
                        }

                        _stream.update { state ->
                            state.copy(
                                status = State.Status.Success,
                                connectingDevices = devices,
                            )
                        }

                        val selectedDevice =
                            if (selectedDevice.isNotBlank() && devices.contains(selectedDevice)) selectedDevice
                            else devices.firstOrNull() ?: ""

                        selectDevice(selectedDevice)
                    }
                }
        }
    }

    fun importAdb(adbPath: String?) {
        adbPath ?: return
        scope.launch {
            adbDataStore.updateData { it.copy(toolPath = adbPath) }
        }
    }

    fun selectDevice(deviceName: String) {
        scope.launch {
            _stream.update {
                it.copy(
                    status = if (deviceName.isNotBlank()) State.Status.Success
                    else State.Status.Fail("未连接设备或设备已断开"),
                    selectedDevice = deviceName
                )
            }
        }
    }

    fun formatIfAdbCmd(input: String): String {
        if (!input.startsWith("adb")) return input

        //是否有选中设备
        var command = input
        if (selectedDevice.isNotBlank()) {
            command = input.replace("adb", "adb -s $selectedDevice")
        }

        //兼容Windows
        if (System.getProperty("os.name").contains("Windows")) {
            command = command.replace("adb", "cmd /c adb").replace("grep", "findstr")
        }

        return "$adbParentPath/$command"
    }

    private suspend fun autoFindAdbTool() {
        // 尝试从环境变量中获取adb路径
        _stream.update { it.copy(status = State.Status.Loading("正在获取有效adb工具")) }
        val env = System.getenv()
        val path = env["PATH"] ?: ""
        val home = System.getProperty("user.home")

        // 使用whereis命令获取路径，并确保加载完整的shell环境
        val androidSdkPath = "${home}/Library/Android/sdk"
        val platformToolsPath = "$androidSdkPath/platform-tools"
        val newPath =
            "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:$platformToolsPath:$path"

        val shellCommand = """
                export PATH='$newPath'
                whereis adb
            """.trimIndent()

        val adbPath = shellCommand.simpleShell().substringAfter("adb:", "").trim()
        _stream.update { it.copy(status = State.Status.Loading("adb路径: $adbPath")) }

        importAdb(adbPath)
    }
}

data class State(
    val status: Status = Status.Init,
    val adbToolPath: String = "", //adb工具路径
    val adbToolAvailable: Boolean = false, //adb工具是否可用
    val connectingDevices: List<String> = emptyList(), //正在连接的adb备
    val selectedDevice: String = "", //选中连接的adb设备
) {
    sealed class Status(open val text: String = "") {
        object Init : Status("")
        data class Loading(override val text: String) : Status(text)
        data object Success : Status("")
        data class Fail(override val text: String) : Status(text)
    }
}