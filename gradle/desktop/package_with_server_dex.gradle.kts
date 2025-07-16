import org.gradle.api.tasks.Copy
import java.io.File

// 添加一个任务，将server/classes.dex文件复制到桌面应用的资源目录中
tasks.register<Copy>("copyServerDexToDesktopResources") {
    description = "将server/classes.dex文件复制到桌面应用的资源目录中"
    group = "sophon"
    
    // 在配置时捕获需要的值，避免在执行时访问project
    val sourceDir = project.rootProject.file("composeApp/src/desktopMain/server")
    val destDir = project.layout.buildDirectory.dir("resources/desktop/server").get().asFile
    val destDirPath = destDir.absolutePath
    
    from(sourceDir)
    into(destDir)
    include("classes.dex")
    
    // 添加日志
    doFirst {
        println("正在将classes.dex复制到资源目录: ${destDirPath}")
    }
    
    doLast {
        val destFile = File(destDirPath, "classes.dex")
        if (destFile.exists()) {
            println("文件复制成功: ${destFile.absolutePath}, 大小: ${destFile.length()} 字节")
        } else {
            println("警告: 文件复制失败，目标文件不存在: ${destFile.absolutePath}")
        }
    }
}

// 将server/classes.dex文件复制到distribution目录
tasks.register<Copy>("copyServerDexToDistribution") {
    description = "将server/classes.dex文件复制到distribution目录"
    group = "sophon"
    
    // 依赖前一个任务
    dependsOn("copyServerDexToDesktopResources")
    
    // 在配置时捕获需要的值，避免在执行时访问project
    val sourceDir = project.rootProject.file("composeApp/src/desktopMain/server")
    val destDir = project.layout.buildDirectory.dir("compose/binaries/main/app/server").get().asFile
    val destDirPath = destDir.absolutePath
    
    from(sourceDir)
    into(destDir)
    include("classes.dex")
    
    // 添加日志
    doFirst {
        println("正在将classes.dex复制到distribution目录: ${destDirPath}")
    }
    
    doLast {
        val destFile = File(destDirPath, "classes.dex")
        if (destFile.exists()) {
            println("文件复制成功: ${destFile.absolutePath}, 大小: ${destFile.length()} 字节")
        } else {
            println("警告: 文件复制失败，目标文件不存在: ${destFile.absolutePath}")
        }
    }
}

// 对于macOS，我们需要特殊处理，因为应用程序被打包为.app格式
tasks.register("copyServerDexToMacOSApp") {
    description = "将server/classes.dex文件复制到macOS应用程序包中"
    group = "sophon"
    
    // 依赖前一个任务
    dependsOn("copyServerDexToDesktopResources")
    
    // 在配置时捕获需要的值，避免在执行时访问project
    val sourceFilePath = project.rootProject.file("composeApp/src/desktopMain/server/classes.dex").absolutePath
    val destDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon/Contents/server").get().asFile.absolutePath
    
    // 使用doLast而不是Copy任务类型，以避免长时间执行
    doLast {
        val sourceFile = File(sourceFilePath)
        val destDir = File(destDirPath)
        val destFile = File(destDir, sourceFile.name)
        
        // 确保源文件存在
        if (!sourceFile.exists()) {
            println("源文件不存在: ${sourceFile.absolutePath}")
            return@doLast
        }
        
        // 确保目标目录存在
        if (!destDir.exists()) {
            println("创建目标目录: ${destDir.absolutePath}")
            destDir.mkdirs()
        }
        
        // 使用Java NIO Files API复制文件，更可靠
        println("正在复制文件: ${sourceFile.absolutePath} -> ${destFile.absolutePath}")
        try {
            java.nio.file.Files.copy(
                sourceFile.toPath(),
                destFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
            println("文件复制完成: ${destFile.absolutePath}")
            
            // 验证文件是否已复制
            if (destFile.exists() && destFile.length() > 0) {
                println("文件复制验证成功: ${destFile.absolutePath}, 大小: ${destFile.length()} 字节")
            } else {
                println("警告: 文件复制验证失败，目标文件不存在或大小为0: ${destFile.absolutePath}")
            }
        } catch (e: Exception) {
            println("错误: 复制文件时发生异常: ${e.message}")
            e.printStackTrace()
        }
    }
}

// 修改compose desktop应用的打包配置，确保server/classes.dex文件被包含在内
afterEvaluate {
    // 获取compose desktop的打包任务
    tasks.matching { task ->
        task.name == "createDistributable" || task.name.startsWith("package") && task.name.endsWith("DistributionForCurrentOS")
    }.configureEach {
        dependsOn("copyServerDexToDistribution")
        dependsOn("copyServerDexToMacOSApp")
        
        // 确保在打包任务执行前，目标目录已经创建
        // 在配置时捕获需要的值，避免在执行时访问project
        val serverDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon/Contents/server").get().asFile.absolutePath
        
        doFirst {
            val serverDir = File(serverDirPath)
            if (!serverDir.exists()) {
                println("在打包任务中创建server目录: ${serverDir.absolutePath}")
                serverDir.mkdirs()
            }
        }
        
        // 确保在打包任务执行后，文件被复制到目标目录
        // 在配置时捕获需要的值，避免在执行时访问project
        val finalSourceFilePath = project.rootProject.file("composeApp/src/desktopMain/server/classes.dex").absolutePath
        val finalDestDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon/Contents/server").get().asFile.absolutePath
        
        doLast {
            val sourceFile = File(finalSourceFilePath)
            val destDir = File(finalDestDirPath)
            val destFile = File(destDir, sourceFile.name)
            
            // 确保源文件存在
            if (!sourceFile.exists()) {
                println("源文件不存在: ${sourceFile.absolutePath}")
                return@doLast
            }
            
            // 确保目标目录存在
            if (!destDir.exists()) {
                println("创建目标目录: ${destDir.absolutePath}")
                destDir.mkdirs()
            }
            
            // 使用Java NIO Files API复制文件
            println("在打包任务完成后复制文件: ${sourceFile.absolutePath} -> ${destFile.absolutePath}")
            try {
                java.nio.file.Files.copy(
                    sourceFile.toPath(),
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                println("文件复制完成: ${destFile.absolutePath}")
                
                // 验证文件是否已复制
                if (destFile.exists() && destFile.length() > 0) {
                    println("文件复制验证成功: ${destFile.absolutePath}, 大小: ${destFile.length()} 字节")
                } else {
                    println("警告: 文件复制验证失败，目标文件不存在或大小为0: ${destFile.absolutePath}")
                }
            } catch (e: Exception) {
                println("错误: 复制文件时发生异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}