import org.gradle.api.tasks.Copy
import java.io.File

// 添加一个任务，将server/*.dex文件复制到桌面应用的资源目录中
tasks.register<Copy>("copyServerDexToDesktopResources") {
    description = "将server/*.dex文件复制到桌面应用的资源目录中"
    group = "sophon"
    
    // 在配置时捕获需要的值，避免在执行时访问project
    val sourceDir = project.rootProject.file("composeApp/src/desktopMain/server")
    val destDir = project.layout.buildDirectory.dir("resources/desktop/server").get().asFile
    val destDirPath = destDir.absolutePath
    
    from(sourceDir)
    into(destDir)
    include("*.dex")
    
    // 添加日志
    doFirst {
        println("正在将*.dex文件复制到资源目录: ${destDirPath}")
    }
    
    doLast {
        // 检查目标目录中是否有.dex文件
        val dexFiles = File(destDirPath).listFiles { file -> file.name.endsWith(".dex") }
        if (dexFiles != null && dexFiles.isNotEmpty()) {
            dexFiles.forEach { file ->
                println("文件复制成功: ${file.absolutePath}, 大小: ${file.length()} 字节")
            }
        } else {
            println("警告: 没有.dex文件被复制到目标目录: ${destDirPath}")
        }
    }
}

// 将server/*.dex文件复制到distribution目录
tasks.register<Copy>("copyServerDexToDistribution") {
    description = "将server/*.dex文件复制到distribution目录"
    group = "sophon"
    
    // 依赖前一个任务
    dependsOn("copyServerDexToDesktopResources")
    
    // 在配置时捕获需要的值，避免在执行时访问project
    val sourceDir = project.rootProject.file("composeApp/src/desktopMain/server")
    val destDir = project.layout.buildDirectory.dir("compose/binaries/main/app/server").get().asFile
    val destDirPath = destDir.absolutePath
    
    from(sourceDir)
    into(destDir)
    include("*.dex")
    
    // 添加日志
    doFirst {
        println("正在将*.dex文件复制到distribution目录: ${destDirPath}")
    }
    
    doLast {
        // 检查目标目录中是否有.dex文件
        val dexFiles = File(destDirPath).listFiles { file -> file.name.endsWith(".dex") }
        if (dexFiles != null && dexFiles.isNotEmpty()) {
            dexFiles.forEach { file ->
                println("文件复制成功: ${file.absolutePath}, 大小: ${file.length()} 字节")
            }
        } else {
            println("警告: 没有.dex文件被复制到目标目录: ${destDirPath}")
        }
    }
}

// 对于macOS，我们需要特殊处理，因为应用程序被打包为.app格式
tasks.register("copyServerDexToMacOSApp") {
    description = "将server/*.dex文件复制到macOS应用程序包中"
    group = "sophon"
    
    // 依赖前一个任务
    dependsOn("copyServerDexToDesktopResources")
    
    // 在配置时捕获需要的值，避免在执行时访问project
    val sourceDirPath = project.rootProject.file("composeApp/src/desktopMain/server").absolutePath
    // 同时复制到两个可能的目标位置
    val destDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon.app/Contents/server").get().asFile.absolutePath
    
    // 使用doLast而不是Copy任务类型，以避免长时间执行
    doLast {
        val sourceDir = File(sourceDirPath)
        val destDir = File(destDirPath)
        
        // 确保源目录存在
        if (!sourceDir.exists()) {
            println("源目录不存在: ${sourceDir.absolutePath}")
            return@doLast
        }
        
        // 获取所有.dex文件
        val dexFiles = sourceDir.listFiles { file -> file.name.endsWith(".dex") }
        if (dexFiles == null || dexFiles.isEmpty()) {
            println("没有找到.dex文件在: ${sourceDir.absolutePath}")
            return@doLast
        }
        
        // 复制到目标目录
        if (!destDir.exists()) {
            println("创建目标目录: ${destDir.absolutePath}")
            destDir.mkdirs()
        }
        
        // 使用Java NIO Files API复制所有.dex文件到两个目标目录，更可靠
        for (sourceFile in dexFiles) {
            // 复制到目标目录
            val destFile = File(destDir, sourceFile.name)
            println("正在复制文件到目标: ${sourceFile.absolutePath} -> ${destFile.absolutePath}")
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
                println("错误: 复制文件到目标时发生异常: ${e.message}")
                e.printStackTrace()
            }
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
        val serverDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon.app/Contents/server").get().asFile.absolutePath
        
        doFirst {
            val serverDir = File(serverDirPath)
            if (!serverDir.exists()) {
                println("在打包任务中创建server目录: ${serverDir.absolutePath}")
                serverDir.mkdirs()
            }
        }
        
        // 确保在打包任务执行后，文件被复制到目标目录
        // 在配置时捕获需要的值，避免在执行时访问project
        val finalSourceDirPath = project.rootProject.file("composeApp/src/desktopMain/server").absolutePath
        val finalDestDirPath = project.layout.buildDirectory.dir("compose/binaries/main/app/Sophon.app/Contents/server").get().asFile.absolutePath
        
        doLast {
            val sourceDir = File(finalSourceDirPath)
            val destDir = File(finalDestDirPath)
            
            // 确保源目录存在
            if (!sourceDir.exists()) {
                println("源目录不存在: ${sourceDir.absolutePath}")
                return@doLast
            }
            
            // 获取所有.dex文件
            val dexFiles = sourceDir.listFiles { file -> file.name.endsWith(".dex") }
            if (dexFiles == null || dexFiles.isEmpty()) {
                println("没有找到.dex文件在: ${sourceDir.absolutePath}")
                return@doLast
            }

            // 确保目标目录存在
            if (!destDir.exists()) {
                println("创建目标目录: ${destDir.absolutePath}")
                destDir.mkdirs()
            }
            
            // 使用Java NIO Files API复制所有.dex文件到第一个目标目录
            for (sourceFile in dexFiles) {
                // 复制到第二个目标目录
                val destFile = File(destDir, sourceFile.name)
                println("在打包任务完成后复制文件到目标: ${sourceFile.absolutePath} -> ${destFile.absolutePath}")
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
            
            // 验证两个目标目录中的dex文件
            try {
                // 验证目标目录
                if (destDir.exists()) {
                    val dexFiles = destDir.listFiles { file -> file.name.endsWith(".dex") }
                    if (dexFiles != null && dexFiles.isNotEmpty()) {
                        println("验证成功: 在目标目录 ${destDir.absolutePath} 中找到了 ${dexFiles.size} 个dex文件:")
                        dexFiles.forEach { file ->
                            println("  - ${file.name}")
                        }
                    } else {
                        println("警告: 在目标目录 ${destDir.absolutePath} 中没有找到任何dex文件")
                    }
                } else {
                    println("警告: 目标目录 ${destDir.absolutePath} 不存在")
                }
            } catch (e: Exception) {
                println("验证dex文件时发生错误: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}