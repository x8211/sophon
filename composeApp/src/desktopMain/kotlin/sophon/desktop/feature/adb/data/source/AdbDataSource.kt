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
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val deployedAdb = File("/Applications/Sophon.app/Contents/Resources/tools", "adb")
            if (deployedAdb.exists()) return deployedAdb.absolutePath
        }

        val candidatePaths = listOf(
            "composeApp/src/desktopMain/tools/adb",
            "src/desktopMain/tools/adb",
            "tools/adb"
        )
        return candidatePaths.firstOrNull { File(it).exists() }
            ?: File("composeApp/src/desktopMain/tools/adb").absolutePath
    }
}
