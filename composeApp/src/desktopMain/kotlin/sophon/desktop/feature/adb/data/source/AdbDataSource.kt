package sophon.desktop.feature.adb.data.source

import sophon.desktop.core.Shell.oneshotShell
import java.io.File

class AdbDataSource {

    suspend fun fetchConnectedDevices(adbPath: String): List<String> =
        "$adbPath devices".oneshotShell { result ->
            val pattern = Regex("^([\\w.:_-]+)\\s+device\\s*$", RegexOption.MULTILINE)
            pattern.findAll(result).map { it.groupValues[1] }.toList()
        }

    fun resolveBuiltInAdbPath(): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val adbBinary = if (isWindows) "adb.exe" else "adb"

        // 打包后 appResourcesRootDir 会将 windows/ 或 macos/ 子目录的内容
        // 合并到 compose.application.resources.dir 下（平台前缀被去掉）
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val deployedAdb = File(resourcesDir, "tools/$adbBinary")
            if (deployedAdb.exists()) return deployedAdb.absolutePath
        }

        // 开发模式回退路径
        val platformDir = if (isWindows) "windows" else "macos"
        val candidateDirs = listOf(
            "composeApp/src/desktopMain/appResources/$platformDir/tools",
            "src/desktopMain/appResources/$platformDir/tools",
        )

        return candidateDirs
            .map { File(it, adbBinary) }
            .firstOrNull { it.exists() }?.absolutePath
            ?: File(candidateDirs.first(), adbBinary).absolutePath
    }
}
