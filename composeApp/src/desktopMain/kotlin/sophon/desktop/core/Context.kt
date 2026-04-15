package sophon.desktop.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.adb.data.repository.AdbRepositoryImpl
import sophon.desktop.feature.adb.model.AdbState

object Context {

    private val scope = MainScope()
    private val adbRepository = AdbRepositoryImpl()

    val stream: StateFlow<AdbState> = adbRepository.getAdbState()

    init {
        scope.launch {
            val adbPath = adbRepository.autoFindAdbTool()
            adbRepository.updateAdbPath(adbPath)
            while (true) {
                adbRepository.refreshDevices()
                delay(3000)
            }
        }
    }

    fun selectDevice(deviceName: String) {
        scope.launch {
            adbRepository.selectDevice(deviceName)
        }
    }

    fun formatIfAdbCmd(input: String): String {
        if (!input.startsWith("adb")) return input
        val state = adbRepository.getAdbState().value
        var command = input
        if (state.selectedDevice.isNotBlank()) {
            command = command.replace("adb", "adb -s ${state.selectedDevice}")
        }
        if (System.getProperty("os.name").contains("Windows")) {
            command = command.replace("adb", "cmd /c adb").replace("grep", "findstr")
        }
        val parentPath = state.adbParentPath ?: return command
        println("""format adb cmd:
            |$parentPath
            |$command
            |=========
        """.trimMargin())
        return "$parentPath/$command"
    }
}
