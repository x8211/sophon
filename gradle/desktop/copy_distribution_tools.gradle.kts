
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

// ---------------------------------------------------------------
// macOS：手动将 tools 复制到 .app/Contents/Resources/tools
//   （macOS .app 结构需要此方式，appResourcesRootDir 会额外生效）
// ---------------------------------------------------------------

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

val appName: String by project

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
            .dir("compose/binaries/main/app/${appName}.app/Contents/Resources")
            .map { it.asFile.absolutePath }
    )
}

tasks.register<CopyToolsToAppTask>("copyToolsToMacOSAppRelease") {
    description = "将 tools/macos/ 复制到 Release macOS 应用程序包资源目录中"
    group = "toolbox"
    sourceDir.set(File(toolsSourceDir, "macos"))
    destResourcesDirPath.set(
        project.layout.buildDirectory
            .dir("compose/binaries/main-release/app/${appName}.app/Contents/Resources")
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
// ---------------------------------------------------------------
afterEvaluate {
    val copyDebugTask = tasks.named("copyToolsToMacOSApp")
    val copyReleaseTask = tasks.named("copyToolsToMacOSAppRelease")

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
}
