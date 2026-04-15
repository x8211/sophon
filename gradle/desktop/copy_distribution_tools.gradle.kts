
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

// ---------------------------------------------------------------
// macOS：手动将 tools 复制到 .app/Contents/Resources/tools
//   （macOS .app 结构需要此方式，appResourcesRootDir 会额外生效）
//
// Windows：通过 appResourcesRootDir 机制自动打入安装包
//   src/desktopMain/appResources/windows/tools → 安装目录/app/resources/tools
//   因此只需在构建前将 tools/windows/ 下的二进制同步到该目录即可。
// ---------------------------------------------------------------

// ---------------------------------------------------------------
// Task：将平台专属 tools 同步到 appResourcesRootDir 对应子目录
//
// 用法（在 afterEvaluate 中挂钩到各平台打包任务之前）：
//   - prepareWindowsTools → 源: tools/windows/  目标: appResources/windows/tools/
// ---------------------------------------------------------------

/**
 * 将指定 sourceDir 目录的内容递归同步到 destDir，
 * 并对所有文件赋予可执行权限（Windows 上 setExecutable 调用无害）。
 */
abstract class SyncToolsToAppResourcesTask : DefaultTask() {

    @get:InputDirectory
    @get:Optional
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val destDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val src: File = sourceDir.asFile.orNull ?: run {
            logger.warn("⚠️  Tools源目录未配置，跳过同步")
            return
        }
        if (!src.exists() || src.listFiles()?.isEmpty() != false) {
            logger.lifecycle("ℹ️  Tools源目录为空或不存在，跳过同步: ${src.absolutePath}")
            return
        }
        val dest: File = destDir.asFile.orNull ?: run {
            logger.warn("⚠️  Tools目标目录未配置，跳过同步")
            return
        }
        dest.mkdirs()

        logger.lifecycle("🚀 正在同步 Tools 工具...")
        logger.lifecycle("   源: ${src.absolutePath}")
        logger.lifecycle("   目标: ${dest.absolutePath}")

        src.copyRecursively(dest, overwrite = true) { file, exception ->
            logger.warn("❌ 复制失败: ${file.path}, 错误: ${exception.message}")
            OnErrorAction.SKIP
        }

        dest.walk().filter { it.isFile }.forEach { file ->
            if (!file.setExecutable(true)) {
                logger.warn("⚠️  设置执行权限失败: ${file.name}")
            }
        }

        logger.lifecycle("✅ Tools 同步完成。")
    }
}

/**
 * 用于将 tools 目录复制到 macOS .app 包 Contents/Resources/tools 中，
 * 并赋予执行权限。
 *
 * 支持的触发任务:
 *   - packageDmg / createDistributable         → copyToolsToMacOSApp（Debug）
 *   - packageReleaseDmg / createReleaseDistributable → copyToolsToMacOSAppRelease（Release）
 */
abstract class CopyToolsToAppTask : DefaultTask() {

    @get:InputDirectory
    @get:Optional
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val destResourcesDirPath: Property<String>

    @TaskAction
    fun copyTools() {
        val srcDir: File = sourceDir.asFile.orNull ?: run {
            logger.warn("⚠️  Tools源目录未配置，跳过复制")
            return
        }

        if (!srcDir.exists()) {
            logger.warn("⚠️  Tools源目录不存在，跳过复制: ${srcDir.absolutePath}")
            return
        }

        val resourcesDir = File(destResourcesDirPath.get())
        if (!resourcesDir.exists()) {
            logger.warn("⚠️  目标Resources目录不存在，.app 可能未成功生成: ${resourcesDir.absolutePath}")
            return
        }

        val destDir = File(resourcesDir, "tools")
        if (!destDir.exists()) {
            logger.lifecycle("📁 创建Tools工具目标目录: ${destDir.absolutePath}")
            destDir.mkdirs()
        }

        logger.lifecycle("🚀 正在复制Tools工具（macOS）...")
        logger.lifecycle("   源: ${srcDir.absolutePath}")
        logger.lifecycle("   目标: ${destDir.absolutePath}")

        srcDir.copyRecursively(destDir, overwrite = true) { file, exception ->
            logger.warn("❌ 复制失败: ${file.path}, 错误: ${exception.message}")
            OnErrorAction.SKIP
        }

        destDir.walk().filter { it.isFile }.forEach { file ->
            if (!file.setExecutable(true)) {
                logger.warn("⚠️  设置执行权限失败: ${file.name}")
            }
        }

        logger.lifecycle("✅ Tools工具复制完成（macOS）。")
    }
}

// ---------------------------------------------------------------
// 公共路径定义
// ---------------------------------------------------------------

/** 源目录根: composeApp/src/desktopMain/tools */
val toolsSourceDir: File = project.rootProject.file("composeApp/src/desktopMain/tools")

/**
 * appResourcesRootDir 根目录：composeApp/src/desktopMain/appResources
 *
 * 子目录说明（与 Compose Desktop appResourcesRootDir 约定一致）：
 *   windows/tools/  → Windows 专属工具（打入 MSI/EXE，安装后位于 app/resources/tools）
 *   macos/tools/    → macOS 专属工具（同时也由 CopyToolsToAppTask 处理 .app 包）
 */
val appResourcesDir: File = project.rootProject.file("composeApp/src/desktopMain/appResources")

// ---------------------------------------------------------------
// 注册 Windows 打包前的 tools 同步任务
//
// 策略：
//   tools/windows/ 目录中存放 Windows 平台二进制（adb.exe 等）
//   → 同步到 appResources/windows/tools/
//   → appResourcesRootDir 机制自动将 windows/ 内容打入 MSI/EXE 安装包
//   → 安装后路径: <安装目录>/app/resources/tools/
//   → 运行时通过 System.getProperty("compose.application.resources.dir") 获取
// ---------------------------------------------------------------

tasks.register<SyncToolsToAppResourcesTask>("prepareWindowsTools") {
    description = "将 tools/windows/ 同步到 appResources/windows/tools/（供 MSI/EXE 打包使用）"
    group = "toolbox"
    sourceDir.set(File(toolsSourceDir, "windows"))
    destDir.set(File(appResourcesDir, "windows/tools"))
}

// ---------------------------------------------------------------
// 注册 macOS Debug / Release 复制任务（保留原有逻辑）
//
// macOS 专属工具放在 tools/macos/ 下，通过 CopyToolsToAppTask 复制到
// .app/Contents/Resources/tools，同时 appResourcesRootDir 的 macos/ 子目录
// 也可用于补充资源（二者不冲突）。
// ---------------------------------------------------------------

tasks.register<CopyToolsToAppTask>("copyToolsToMacOSApp") {
    description = "将 tools/macos/ 复制到 Debug macOS 应用程序包资源目录中"
    group = "toolbox"
    sourceDir.set(File(toolsSourceDir, "macos"))
    destResourcesDirPath.set(
        project.layout.buildDirectory
            .dir("compose/binaries/main/app/Sophon.app/Contents/Resources")
            .map { it.asFile.absolutePath }
    )
}

tasks.register<CopyToolsToAppTask>("copyToolsToMacOSAppRelease") {
    description = "将 tools/macos/ 复制到 Release macOS 应用程序包资源目录中"
    group = "toolbox"
    sourceDir.set(File(toolsSourceDir, "macos"))
    destResourcesDirPath.set(
        project.layout.buildDirectory
            .dir("compose/binaries/main-release/app/Sophon.app/Contents/Resources")
            .map { it.asFile.absolutePath }
    )
}

// ---------------------------------------------------------------
// afterEvaluate：建立任务依赖链
//
// macOS Debug:
//   createDistributable → copyToolsToMacOSApp → packageDmg
//
// macOS Release:
//   createReleaseDistributable → copyToolsToMacOSAppRelease → packageReleaseDmg
//
// Windows Debug (MSI):
//   prepareWindowsTools → packageMsi
//
// Windows Release (MSI):
//   prepareWindowsTools → packageReleaseMsi
//
// Windows Debug (EXE):
//   prepareWindowsTools → packageExe
//
// Windows Release (EXE):
//   prepareWindowsTools → packageReleaseExe
// ---------------------------------------------------------------
afterEvaluate {
    val copyDebugTask = tasks.named("copyToolsToMacOSApp")
    val copyReleaseTask = tasks.named("copyToolsToMacOSAppRelease")
    val prepareWindowsTask = tasks.named("prepareWindowsTools")

    // ---------- macOS ----------
    tasks.findByName("packageDmg")?.let { packageDmg ->
        println("配置任务顺序: createDistributable → copyToolsToMacOSApp → packageDmg")
        packageDmg.dependsOn(copyDebugTask)
        copyDebugTask.configure { mustRunAfter(tasks.named("createDistributable")) }
    }

    tasks.findByName("packageReleaseDmg")?.let { packageReleaseDmg ->
        println("配置任务顺序: createReleaseDistributable → copyToolsToMacOSAppRelease → packageReleaseDmg")
        packageReleaseDmg.dependsOn(copyReleaseTask)
        copyReleaseTask.configure { mustRunAfter(tasks.named("createReleaseDistributable")) }
    }

    // ---------- Windows MSI ----------
    tasks.findByName("packageMsi")?.let { pkg ->
        println("配置任务顺序: prepareWindowsTools → packageMsi")
        pkg.dependsOn(prepareWindowsTask)
    }

    tasks.findByName("packageReleaseMsi")?.let { pkg ->
        println("配置任务顺序: prepareWindowsTools → packageReleaseMsi")
        pkg.dependsOn(prepareWindowsTask)
    }

    // ---------- Windows EXE ----------
    tasks.findByName("packageExe")?.let { pkg ->
        println("配置任务顺序: prepareWindowsTools → packageExe")
        pkg.dependsOn(prepareWindowsTask)
    }

    tasks.findByName("packageReleaseExe")?.let { pkg ->
        println("配置任务顺序: prepareWindowsTools → packageReleaseExe")
        pkg.dependsOn(prepareWindowsTask)
    }
}
