
import java.io.File

/**
 * 专门用于将 adb 工具复制到打包后的 macOS 应用程序包中
 * 目标路径: Sophon.app/Contents/Resources/tools/adb
 */

// 对于macOS，我们需要特殊处理，因为应用程序被打包为.app格式
tasks.register("copyAdbToolsToMacOSApp") {
    description = "将tools目录（包含adb）复制到macOS应用程序包资源目录中"
    group = "sophon"

    // 在配置时捕获需要的值，避免在执行时访问project
    // 源目录: composeApp/src/desktopMain/tools
    val sourceDirPath = project.rootProject.file("composeApp/src/desktopMain/tools").absolutePath
    
    // 目标目录: .../Sophon.app/Contents/Resources/tools
    // 注意：compose.application.resources.dir 通常指向 Contents/Resources
    val appResourcesDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon.app/Contents/Resources").get().asFile.absolutePath

    // 使用doLast确保在打包后执行复制
    doLast {
        val sourceDir = File(sourceDirPath)
        val destDir = File(appResourcesDirPath, "tools") // Create 'tools' subdir inside Resources

        // 确保源目录存在
        if (!sourceDir.exists()) {
            println("ADB源目录不存在: ${sourceDir.absolutePath}")
            return@doLast
        }

        // 确保目标目录存在
        if (!destDir.exists()) {
            println("创建ADB工具目标目录: ${destDir.absolutePath}")
            destDir.mkdirs()
        }

        println("正在复制ADB工具...")
        println("源: ${sourceDir.absolutePath}")
        println("目标: ${destDir.absolutePath}")

        // 复制整个目录内容
        sourceDir.copyRecursively(destDir, overwrite = true) { file, exception ->
            println("复制失败: ${file.path}, 错误: ${exception.message}")
            OnErrorAction.SKIP
        }
        
        // 确保adb有执行权限
        val adbFile = File(destDir, "adb")
        if (adbFile.exists()) {
            adbFile.setExecutable(true)
            println("已设置adb执行权限: ${adbFile.absolutePath}")
        } else {
             // 可能是 adb.exe
            val adbExe = File(destDir, "adb.exe")
             if (adbExe.exists()) {
                adbExe.setExecutable(true)
                 println("已设置adb.exe执行权限: ${adbExe.absolutePath}")
             }
        }
        
        println("ADB工具复制完成。")
    }
}

// 修改compose desktop应用的打包配置，确保在打包完成后执行复制
afterEvaluate {
    // 获取compose desktop的打包任务
    tasks.matching { task ->
        task.name == "createDistributable" || (task.name.startsWith("package") && task.name.endsWith("DistributionForCurrentOS"))
    }.configureEach {
        finalizedBy("copyAdbToolsToMacOSApp")
    }
}
