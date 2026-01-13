package sophon.desktop.feature.adb.domain.usecase

import sophon.desktop.feature.adb.domain.model.AdbState

/**
 * 格式化 ADB 命令的用例
 * 该逻辑从 Context 迁移，负责根据当前状态（OS、已选设备）包装命令
 */
class FormatAdbCommandUseCase {
    operator fun invoke(input: String, adbState: AdbState): String {
        if (!input.startsWith("adb")) return input

        var command = input
        val selectedDevice = adbState.selectedDevice

        // 是否有选中设备
        if (selectedDevice.isNotBlank()) {
            command = input.replace("adb", "adb -s $selectedDevice")
        }

        // 兼容 Windows
        if (System.getProperty("os.name").contains("Windows")) {
            command = command.replace("adb", "cmd /c adb").replace("grep", "findstr")
        }

        return "${adbState.adbParentPath}/$command"
    }
}
