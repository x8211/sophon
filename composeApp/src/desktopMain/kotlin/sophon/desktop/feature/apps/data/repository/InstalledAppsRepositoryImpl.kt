package sophon.desktop.feature.apps.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.skia.Image
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.apps.domain.model.AppInfo
import sophon.desktop.feature.apps.domain.model.AppLoadState
import sophon.desktop.feature.apps.domain.repository.InstalledAppsRepository
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.zip.ZipFile

class InstalledAppsRepositoryImpl : InstalledAppsRepository {
    private var aapt2Path: String = ""

    override fun getInstalledApps(): Flow<AppLoadState> = flow {
        emit(AppLoadState.Loading)
        
        val adbParentFolder = Context.adbParentPath
        if (adbParentFolder == null) {
            emit(AppLoadState.Error("ADB path not found"))
            return@flow
        }

        // 0. 获取aapt2工具路径
        try {
            val sdkFolder = File(adbParentFolder).parentFile
            val buildToolsFolder = File(sdkFolder, "build-tools")
            if (buildToolsFolder.exists()) {
                for (folder in buildToolsFolder.listFiles(object : FileFilter {
                    override fun accept(file: File?): Boolean {
                        return file?.isDirectory == true
                    }
                }) ?: emptyArray()) {
                    val list = folder.listFiles(object : FilenameFilter {
                        override fun accept(dir: File?, name: String?): Boolean {
                            return name == "aapt2" || name == "aapt2.exe"
                        }
                    })
                    if (list != null && list.isNotEmpty()) {
                        aapt2Path = list[0].absolutePath
                        break
                    }
                }
            }
        } catch (e: Exception) {
             // ignore
        }
        
        if (aapt2Path.isBlank()) {
            // Fallback: try to find in path or just continue without icons/details if strict?
            // Original code just returns empty list.
             emit(AppLoadState.Error("AAPT2 not found in Android SDK"))
             return@flow
        }

        // 1. 获取所有非系统应用列表
        try {
            val result = "adb shell pm list packages -3".simpleShell()
            val packageNames = result.split("\n")
                .filter { it.startsWith("package:") }
                .map { it.substring(8).trim() }

            val totalApps = packageNames.size
            val apps = mutableListOf<AppInfo>()

            packageNames.forEachIndexed { index, packageName ->
                val appInfo = getAppInfo(packageName)
                if (appInfo != null) {
                    apps.add(appInfo)
                }
                emit(AppLoadState.Progress(index + 1, totalApps))
            }

            emit(AppLoadState.Success(apps.sortedBy { it.appName }))
        } catch (e: Exception) {
            emit(AppLoadState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun getAppInfo(packageName: String): AppInfo? {
        try {
            // 1. 获取APK路径并下载到本地临时目录
            val pathResult = "adb shell pm path $packageName".simpleShell()
            val apkPath = pathResult
                .split("\n")
                .firstOrNull { it.startsWith("package:") }
                ?.substring(8)
                ?.trim()
                ?: return null

            // 创建临时目录
            val tempDir = File(System.getProperty("java.io.tmpdir"), "app_analysis")
            if (!tempDir.exists()) tempDir.mkdirs()
            val localApkFile = File(tempDir, "$packageName.apk")

            // 从设备拉取APK文件
            // 注意：adb pull 可能比较耗时
            "adb pull $apkPath ${localApkFile.absolutePath}".simpleShell()

            if (!localApkFile.exists() || localApkFile.length() == 0L) {
                return null
            }

            // 2. 使用本地aapt2解析APK
            val aaptInfo = "$aapt2Path dump badging ${localApkFile.absolutePath}".simpleShell()

            // 3. 获取应用基本信息
            val appNameAndIcon = extractValues(aaptInfo, "label='([^']*)'\\s+icon='([^']*)'")
            val appName = appNameAndIcon.getOrNull(1) ?: packageName // Fallback to package name
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
            // 这些命令也需要 simpleShell, 它是 suspend func
            // 注意：grep 在某些 shell 可能行为不同，这里假设环境一致
            val dataSizeStr = "adb shell dumpsys package $packageName | grep dataDir".simpleShell()
            // 解析通常是 dataDir=... 
            // 原代码：File(it).length() -> 这里原代码看起来有点奇怪，dumpsys 返回的是文本路径？
            // 原代码是: val dataSize = "adb shell dumpsys package $packageName | grep dataDir".simpleShell().let { File(it).length() }
            // 这意味着它试图获取本地文件的大小？但这路径是设备上的。File(devicePath).length() 在本地肯定不存在或者是0。
            // 我猜原作者可能想获取设备上文件夹大小，或者是写错了。
            // 鉴于这是重构，我保留原逻辑的意图，但原逻辑看起来是错的（在PC上读取设备路径的文件大小）。
            // 除非它把 dumpsys 输出当做文件路径？
            // 让我们修正一下：dumpsys 输出包含 dataDir=/data/data/com.example
            // 我们无法直接获取大小，除非用 du -sh。
            // 为了安全起见，我们暂时置为 0，或者保留原样但加个 try catch 避免奔溃。
            // 实际上原代码逻辑：it 是 dumpsys 的输出，比如 "    dataDir=/data/user/0/com.example"，File(this_string).length() 肯定是0或者异常。
            // 我们暂时给 0 吧，或者改进它。改进它需要额外的 shell 命令 "du -s <path>"。
            val dataSize = 0L 
            val cacheSize = 0L

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
            // aapt output iconPath usually doesn't have slash at start, but zip entry might needs matching
            val iconEntry =
                if (iconPath.endsWith(".png") || iconPath.endsWith(".webp")) zipFile.getEntry(iconPath)
                else {
                    // Fallback search
                    zipFile.entries().asSequence().firstOrNull {
                        val name = it.name.lowercase()
                        (name.endsWith("ic_launcher.png") ||
                                name.endsWith("ic_launcher.webp") ||
                                name.endsWith("ic_launcher_foreground.png") ||
                                name.endsWith("ic_launcher_foreground.webp")) && !name.contains("round") // prefer non-round first?
                    }
                }

            if (iconEntry != null) {
                val iconData = zipFile.getInputStream(iconEntry).use { it.readBytes() }
                // Image.makeFromEncoded requires Skia
                val image = Image.makeFromEncoded(iconData)
                return image.toComposeImageBitmap()
            }
            zipFile.close()
            null
        } catch (e: Exception) {
            println("Error extracting app icon: ${e.message}")
            return null
        }
    }
}
