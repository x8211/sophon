package sophon.desktop.feature.apps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.simpleShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.zip.ZipFile

class InstalledAppsRepository {
    private var aapt2Path: String = ""

    suspend fun getInstalledApps(onProgress: (Int, Int) -> Unit = { _, _ -> }): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val adbParentFolder = Context.adbParentPath ?: return@withContext emptyList()

            // 0. 获取aapt2工具路径
            val sdkFolder = File(adbParentFolder).parentFile
            val buildToolsFolder = File(sdkFolder, "build-tools")
            for (folder in buildToolsFolder.listFiles(object : FileFilter {
                override fun accept(file: File?): Boolean {
                    return file?.isDirectory == true
                }
            })) {
                val list = folder.listFiles(object : FilenameFilter {
                    override fun accept(dir: File?, name: String?): Boolean {
                        return name == "aapt2"
                    }
                })
                if (list.isNotEmpty()) {
                    aapt2Path = list[0].absolutePath
                    break
                }
            }
            if (aapt2Path.isBlank()) return@withContext emptyList()

            // 1. 获取所有非系统应用列表
            val result = "adb shell pm list packages -3".simpleShell()
            val packageNames = result.split("\n")
                .filter { it.startsWith("package:") }
                .map { it.substring(8) }

            val totalApps = packageNames.size
            val apps = mutableListOf<AppInfo>()

            packageNames.forEachIndexed { index, packageName ->
                val appInfo = getAppInfo(packageName)
                if (appInfo != null) {
                    apps.add(appInfo)
                }
                onProgress(index + 1, totalApps)
            }

            apps.sortedBy { it.appName }
        }

    private suspend fun getAppInfo(packageName: String): AppInfo? {
        try {
            // 1. 获取APK路径并下载到本地临时目录
            val apkPath = "adb shell pm path $packageName".simpleShell()
                .split("\n")
                .firstOrNull { it.startsWith("package:") }
                ?.substring(8)
                ?: return null

            // 创建临时目录
            val tempDir = File(System.getProperty("java.io.tmpdir"), "app_analysis")
            tempDir.mkdirs()
            val localApkFile = File(tempDir, "$packageName.apk")

            // 从设备拉取APK文件
            "adb pull $apkPath ${localApkFile.absolutePath}".simpleShell()

            // 2. 使用本地aapt2解析APK
            val aaptInfo = "$aapt2Path dump badging ${localApkFile.absolutePath}".simpleShell()

            // 3. 获取应用基本信息
            val appNameAndIcon = extractValues(aaptInfo, "label='([^']*)'\\s+icon='([^']*)'")
            val appName = appNameAndIcon.getOrNull(1) ?: ""
            val appIcon = appNameAndIcon.getOrNull(2) ?: ""
            val versionName = extractValue(aaptInfo, "versionName='([^']+)'")
            val versionCode = extractValue(aaptInfo, "versionCode='([^']+)'")

            // 4. 获取SDK相关信息
            val minSdkVersion = extractValue(aaptInfo, "minSdkVersion:'([^']+)'")
            val targetSdkVersion = extractValue(aaptInfo, "targetSdkVersion:'([^']+)'")
            val compileSdkVersion = extractValue(aaptInfo, "compileSdkVersion:'([^']+)'")
            val buildToolsVersion = extractValue(aaptInfo, "buildToolsVersion:'([^']+)'")

            // 5. 获取应用大小信息
            val size = localApkFile.length()
            val dataSize = "adb shell dumpsys package $packageName | grep dataDir".simpleShell()
                .let { File(it).length() }
            val cacheSize = "adb shell dumpsys package $packageName | grep cacheDir".simpleShell()
                .let { File(it).length() }

            // 6. 解析应用图标
            val iconBitmap = extractAppIcon(localApkFile, appIcon)

            // 清理临时文件
            localApkFile.delete()

            return AppInfo(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                path = apkPath,
                icon = iconBitmap,
                minSdkVersion = minSdkVersion,
                targetSdkVersion = targetSdkVersion,
                compileSdkVersion = compileSdkVersion,
                buildToolsVersion = buildToolsVersion,
                dependencies = emptyList(),
                size = size,
                dataSize = dataSize,
                cacheSize = cacheSize,
            )
        } catch (e: Exception) {
            println("Error getting app info for $packageName: ${e.message}")
            return null
        }
    }

    private fun extractValue(text: String, pattern: String): String {
        return try {
            val regex = Regex(pattern)
            regex.find(text)?.groupValues?.get(1) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractValues(text: String, pattern: String): List<String> {
        return try {
            val regex = Regex(pattern)
            regex.find(text)?.groupValues ?: emptyList()
        } catch (_: Exception) {
            emptyList<String>()
        }
    }

    private fun extractAppIcon(apkFile: File, iconPath: String): ImageBitmap? {
        return try {
            val zipFile = ZipFile(apkFile)

            // 1. 先尝试直接根据iconPath加载图标
            val iconEntry =
                if (iconPath.endsWith(".png")) zipFile.getEntry(iconPath)
                else {
                    zipFile.entries().toList().firstOrNull {
                        it.name.endsWith("ic_launcher.png") ||
                                it.name.endsWith("ic_launcher.webp") ||
                                it.name.endsWith("ic_launcher_foreground.png") ||
                                it.name.endsWith("ic_launcher_foreground.webp")
                    }
                }

            if (iconEntry != null) {
                val iconData = zipFile.getInputStream(iconEntry).use { it.readBytes() }
                val image = Image.makeFromEncoded(iconData)
                return image.toComposeImageBitmap()
            }

            null
        } catch (e: Exception) {
            println("Error extracting app icon: ${e.message}")
            null
        }
    }

}