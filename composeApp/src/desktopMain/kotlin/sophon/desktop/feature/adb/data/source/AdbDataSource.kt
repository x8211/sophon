package sophon.desktop.feature.adb.data.source

import sophon.desktop.core.Shell.oneshotShell
import java.io.File

class AdbDataSource {

    suspend fun fetchConnectedDevices(adbPath: String): List<String> =
        "$adbPath devices".oneshotShell { result ->
            val pattern = Regex("^([a-zA-Z0-9-]+)\\s+device$", RegexOption.MULTILINE)
            pattern.findAll(result).map { it.groupValues[1] }.toList()
        }

    fun resolveBuiltInAdbPath(): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val adbBinary = if (isWindows) "adb.exe" else "adb"

        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            if (isWindows) {
                val deployedAdb = File(resourcesDir, "tools/windows/$adbBinary")
                if (deployedAdb.exists()) return deployedAdb.absolutePath
            } else {
                val deployedAdb = File("/Applications/Sophon.app/Contents/Resources/tools", adbBinary)
                if (deployedAdb.exists()) return deployedAdb.absolutePath
            }
        }

        val candidateDirs = if (isWindows) {
            listOf(
                "composeApp/src/desktopMain/tools/windows",
                "src/desktopMain/tools/windows",
                "tools/windows"
            )
        } else {
            listOf(
                "composeApp/src/desktopMain/tools",
                "src/desktopMain/tools",
                "tools"
            )
        }

        return candidateDirs
            .map { File(it, adbBinary) }
            .firstOrNull { it.exists() }?.absolutePath
            ?: File(candidateDirs.first(), adbBinary).absolutePath
    }
}
