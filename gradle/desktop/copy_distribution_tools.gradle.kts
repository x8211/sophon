
import java.io.File

/**
 * 专门用于将 tools 目录下的所有工具复制到打包后的 macOS 应用程序包中
 * 并为所有打包后的文件赋予执行权限
 * 目标路径: Sophon.app/Contents/Resources/tools/
 */

// 对于macOS，我们需要特殊处理，因为应用程序被打包为.app格式
tasks.register("copyToolsToMacOSApp") {
    description = "将tools目录（包含所有工具）复制到macOS应用程序包资源目录中"
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
            println("Tools源目录不存在: ${sourceDir.absolutePath}")
            return@doLast
        }

        // 确保目标目录存在
        if (!destDir.exists()) {
            println("创建Tools工具目标目录: ${destDir.absolutePath}")
            destDir.mkdirs()
        }

        println("正在复制Tools工具...")
        println("源: ${sourceDir.absolutePath}")
        println("目标: ${destDir.absolutePath}")

        // 复制整个目录内容
        sourceDir.copyRecursively(destDir, overwrite = true) { file, exception ->
            println("复制失败: ${file.path}, 错误: ${exception.message}")
            OnErrorAction.SKIP
        }
        
        // 遍历目标目录下的所有文件并赋予执行权限
        destDir.walk().forEach { file ->
            if (file.isFile) {
                if (file.setExecutable(true)) {
                    println("已设置执行权限: ${file.name}")
                } else {
                    println("设置执行权限失败: ${file.name}")
                }
            }
        }
        
        println("Tools工具复制完成。")
    }
}

// 修改compose desktop应用的打包配置，确保在打包完成后执行复制
afterEvaluate {
    // 获取compose desktop的打包任务
    tasks.matching { task ->
        task.name == "createDistributable" || (task.name.startsWith("package") && task.name.endsWith("DistributionForCurrentOS"))
    }.configureEach {
        finalizedBy("copyToolsToMacOSApp")
    }
}
