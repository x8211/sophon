
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * 用于将 tools 目录复制到 macOS .app 包 Resources 目录中，并赋予执行权限的 Task 类。
 *
 * 支持的触发任务:
 *   - packageDmg / createDistributable       → copyToolsToMacOSApp（Debug，目录: main）
 *   - packageReleaseDmg / createReleaseDistributable → copyToolsToMacOSAppRelease（Release，目录: main-release）
 *
 * @property sourceDir  源 tools 目录（composeApp/src/desktopMain/tools）
 * @property destDir    目标 Resources 目录（Sophon.app/Contents/Resources）
 */
abstract class CopyToolsToAppTask : DefaultTask() {

    /** 源 tools 目录，通过 Gradle Property 传入，支持配置缓存 */
    @get:InputDirectory
    @get:Optional
    abstract val sourceDir: DirectoryProperty

    /** 目标 Resources 目录（Sophon.app/Contents/Resources），通过 Gradle Property 传入 */
    @get:Input
    abstract val destResourcesDirPath: Property<String>

    /**
     * 任务执行入口：将 sourceDir 目录递归复制到 destResourcesDirPath/tools，
     * 并为所有文件赋予可执行权限。
     */
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

        logger.lifecycle("🚀 正在复制Tools工具...")
        logger.lifecycle("   源: ${srcDir.absolutePath}")
        logger.lifecycle("   目标: ${destDir.absolutePath}")

        // 递归复制整个 tools 目录内容
        srcDir.copyRecursively(destDir, overwrite = true) { file, exception ->
            logger.warn("❌ 复制失败: ${file.path}, 错误: ${exception.message}")
            OnErrorAction.SKIP
        }

        // 遍历目标目录下的所有文件并赋予执行权限
        destDir.walk().forEach { file ->
            if (file.isFile) {
                if (file.setExecutable(true)) {
                    logger.lifecycle("✅ 已设置执行权限: ${file.name}")
                } else {
                    logger.warn("⚠️  设置执行权限失败: ${file.name}")
                }
            }
        }

        logger.lifecycle("✅ Tools工具复制完成。")
    }
}

// ---------------------------------------------------------------
// 注册 Debug 和 Release 两个复制任务
// ---------------------------------------------------------------

/** 源目录: composeApp/src/desktopMain/tools */
val toolsSourceDir: File = project.rootProject.file("composeApp/src/desktopMain/tools")

/**
 * Debug 包 tools 复制任务（对应 packageDmg / createDistributable）
 * .app 位于 compose/binaries/main/app/Sophon.app
 */
tasks.register<CopyToolsToAppTask>("copyToolsToMacOSApp") {
    description = "将tools目录复制到 Debug macOS 应用程序包资源目录中"
    group = "sophon"
    // 使用 DirectoryProperty 传递路径，避免脚本对象引用
    sourceDir.set(toolsSourceDir)
    destResourcesDirPath.set(
        project.layout.buildDirectory
            .dir("compose/binaries/main/app/Sophon.app/Contents/Resources")
            .map { it.asFile.absolutePath }
    )
}

/**
 * Release 包 tools 复制任务（对应 packageReleaseDmg / createReleaseDistributable）
 * .app 位于 compose/binaries/main-release/app/Sophon.app
 */
tasks.register<CopyToolsToAppTask>("copyToolsToMacOSAppRelease") {
    description = "将tools目录复制到 Release macOS 应用程序包资源目录中"
    group = "sophon"
    sourceDir.set(toolsSourceDir)
    destResourcesDirPath.set(
        project.layout.buildDirectory
            .dir("compose/binaries/main-release/app/Sophon.app/Contents/Resources")
            .map { it.asFile.absolutePath }
    )
}

// ---------------------------------------------------------------
// 在所有项目配置完成后，建立正确的任务执行顺序：
//
// Debug:
//   createDistributable → copyToolsToMacOSApp → packageDmg
//
// Release:
//   createReleaseDistributable → copyToolsToMacOSAppRelease → packageReleaseDmg
//
// 说明：tools 必须在 .app 生成后（createDistributable完成）且 DMG 打包前复制，
//       否则 DMG 中不包含 tools 工具。
// ---------------------------------------------------------------
afterEvaluate {
    val copyDebugTask = tasks.named("copyToolsToMacOSApp")
    val copyReleaseTask = tasks.named("copyToolsToMacOSAppRelease")

    // Debug 链：packageDmg 依赖 copyToolsToMacOSApp，
    //           copyToolsToMacOSApp 必须在 createDistributable 之后运行
    tasks.findByName("packageDmg")?.let { packageDmg ->
        println("配置任务顺序: createDistributable → copyToolsToMacOSApp → packageDmg")
        packageDmg.dependsOn(copyDebugTask)
        copyDebugTask.configure {
            mustRunAfter(tasks.named("createDistributable"))
        }
    }

    // Release 链：packageReleaseDmg 依赖 copyToolsToMacOSAppRelease，
    //             copyToolsToMacOSAppRelease 必须在 createReleaseDistributable 之后运行
    tasks.findByName("packageReleaseDmg")?.let { packageReleaseDmg ->
        println("配置任务顺序: createReleaseDistributable → copyToolsToMacOSAppRelease → packageReleaseDmg")
        packageReleaseDmg.dependsOn(copyReleaseTask)
        copyReleaseTask.configure {
            mustRunAfter(tasks.named("createReleaseDistributable"))
        }
    }
}
