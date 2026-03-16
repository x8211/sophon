package sophon.desktop.feature.appmonitor.feature.fileexplorer.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.AppDirectoryInfo
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.FileItem
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.repository.FileExplorerRepository

/**
 * 文件浏览器仓库实现
 * 
 * 使用ADB命令获取Android设备上的文件系统信息
 * 支持高版本Android系统的权限限制,对debuggable应用使用run-as命令
 */
class FileExplorerRepositoryImpl : FileExplorerRepository {
    
    // 缓存当前包名和其debuggable状态
    private var cachedPackageName: String? = null
    private var isDebuggable: Boolean = false
    
    /**
     * 获取指定目录下的文件列表
     * 
     * 策略:
     * 1. 首先尝试直接使用 `adb shell ls -la [path]`
     * 2. 如果失败(权限被拒绝),且应用是debuggable,则使用 `run-as` 命令
     * 3. 如果都失败,抛出异常
     * 
     * @param path 目录路径
     * @return 文件项列表
     */
    override suspend fun getFileList(path: String): List<FileItem> {
        // 尝试方法1: 直接访问
        val directResult = try {
            "adb shell ls -la \"$path\" 2>&1".oneshotShell { output ->
                if (output.contains("Permission denied") || output.contains("No such file")) {
                    null // 权限被拒绝或文件不存在
                } else {
                    parseFileList(output, path)
                }
            }
        } catch (e: Exception) {
            null
        }
        
        if (directResult != null && directResult.isNotEmpty()) {
            return directResult
        }
        
        // 方法1失败,尝试方法2: 使用run-as (仅对debuggable应用有效)
        val packageName = cachedPackageName
        if (packageName != null && isDebuggable) {
            return try {
                "adb shell run-as $packageName ls -la \"$path\" 2>&1".oneshotShell { output ->
                    if (output.contains("Permission denied") || output.contains("Package") && output.contains("is not debuggable")) {
                        throw Exception("应用不是debuggable模式或权限被拒绝")
                    }
                    parseFileList(output, path)
                }
            } catch (e: Exception) {
                throw Exception("无法访问目录: ${e.message}")
            }
        }
        
        throw Exception("无法访问目录,可能是权限限制。请确保应用是debuggable模式。")
    }
    
    /**
     * 获取应用的目录信息
     * 
     * 使用 `adb shell dumpsys package [packageName]` 命令获取应用信息
     * 同时检测应用是否为debuggable模式
     * 
     * @param packageName 应用包名
     * @return 应用目录信息,如果获取失败返回null
     */
    override suspend fun getAppDirectories(packageName: String): AppDirectoryInfo? {
        return "adb shell dumpsys package $packageName".oneshotShell { output ->
            // 缓存包名
            cachedPackageName = packageName
            
            // 检测是否为debuggable
            isDebuggable = output.contains("flags=") && 
                          (output.contains("DEBUGGABLE") || 
                           checkDebuggableFlag(output))
            
            parseAppDirectories(output, packageName)
        }
    }
    
    /**
     * 检查应用的flags字段是否包含debuggable标志
     * 
     * flags字段示例: flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]
     * 或者十六进制: flags=0x82 (bit 1表示debuggable)
     * 
     * @param output dumpsys输出
     * @return 是否为debuggable
     */
    private fun checkDebuggableFlag(output: String): Boolean {
        // 查找flags行
        val flagsLine = output.lines().find { it.trim().startsWith("flags=") } ?: return false
        
        // 方法1: 检查是否包含DEBUGGABLE文本
        if (flagsLine.contains("DEBUGGABLE")) {
            return true
        }
        
        // 方法2: 检查十六进制flags值的bit 1
        val hexMatch = Regex("flags=0x([0-9a-fA-F]+)").find(flagsLine)
        if (hexMatch != null) {
            val flagsValue = hexMatch.groupValues[1].toLongOrNull(16) ?: 0
            // ApplicationInfo.FLAG_DEBUGGABLE = 0x2 (bit 1)
            return (flagsValue and 0x2) != 0L
        }
        
        return false
    }
    
    /**
     * 解析ls -la命令的输出
     * 
     * 输出格式示例:
     * drwxrwx--x 3 u0_a123 u0_a123 4096 2024-01-01 12:00 cache
     * -rw-rw---- 1 u0_a123 u0_a123 1024 2024-01-01 12:00 file.txt
     * 
     * 注意: run-as命令的ls输出可能格式略有不同,需要兼容处理
     * 
     * @param output 命令输出
     * @param basePath 基础路径
     * @return 解析后的文件项列表
     */
    private fun parseFileList(output: String, basePath: String): List<FileItem> {
        val items = mutableListOf<FileItem>()
        val lines = output.lines()
        
        for (line in lines) {
            if (line.isBlank()) continue
            
            // 跳过总计行和错误信息
            if (line.startsWith("total") || 
                line.contains("Permission denied") ||
                line.contains("No such file")) continue
            
            val parts = line.trim().split(Regex("\\s+"))
            
            // 兼容不同格式的ls输出
            // 标准格式: drwxrwx--x 3 u0_a123 u0_a123 4096 2024-01-01 12:00 cache
            // 简化格式: drwxrwx--x cache (某些设备的run-as ls输出)
            
            if (parts.size < 2) continue
            
            val permissions = parts[0]
            val name: String
            val owner: String
            val group: String
            val sizeStr: String
            val date: String
            val time: String
            
            when {
                parts.size >= 8 -> {
                    // 标准格式
                    owner = parts[2]
                    group = parts[3]
                    sizeStr = parts[4]
                    date = parts[5]
                    time = parts[6]
                    name = parts.drop(7).joinToString(" ")
                }
                parts.size >= 5 -> {
                    // 中等格式 (没有owner/group)
                    owner = "unknown"
                    group = "unknown"
                    sizeStr = parts[1]
                    date = parts[2]
                    time = parts[3]
                    name = parts.drop(4).joinToString(" ")
                }
                else -> {
                    // 简化格式 (只有权限和名称)
                    owner = "unknown"
                    group = "unknown"
                    sizeStr = "0"
                    date = ""
                    time = ""
                    name = parts.drop(1).joinToString(" ")
                }
            }
            
            // 跳过 . 和 .. 目录
            if (name == "." || name == "..") continue
            
            val isDirectory = permissions.startsWith("d")
            val size = if (isDirectory) null else sizeStr.toLongOrNull()
            val fullPath = if (basePath.endsWith("/")) {
                "$basePath$name"
            } else {
                "$basePath/$name"
            }
            
            items.add(
                FileItem(
                    name = name,
                    path = fullPath,
                    isDirectory = isDirectory,
                    size = size,
                    permissions = permissions,
                    owner = owner,
                    group = group,
                    modifiedTime = if (date.isNotEmpty()) "$date $time" else "未知"
                )
            )
        }
        
        return items
    }
    
    /**
     * 解析dumpsys package命令的输出,提取应用目录信息
     * 
     * 从输出中查找以下字段:
     * - dataDir=/data/user/0/[packageName]
     * - codePath=/data/app/[packageName]
     * 
     * @param output 命令输出
     * @param packageName 应用包名
     * @return 应用目录信息,如果解析失败返回null
     */
    private fun parseAppDirectories(output: String, packageName: String): AppDirectoryInfo? {
        var dataDir: String? = null
        var cacheDir: String? = null
        
        val lines = output.lines()
        for (line in lines) {
            val trimmed = line.trim()
            
            // 查找 dataDir
            if (trimmed.startsWith("dataDir=")) {
                dataDir = trimmed.substringAfter("dataDir=")
            }
        }
        
        // 如果找到了dataDir,构建缓存目录路径
        if (dataDir != null) {
            cacheDir = "$dataDir/cache"
            val externalCacheDir = "/sdcard/Android/data/$packageName/cache"
            
            return AppDirectoryInfo(
                packageName = packageName,
                dataDir = dataDir,
                cacheDir = cacheDir,
                externalCacheDir = externalCacheDir,
                isDebuggable = isDebuggable // 使用缓存的debuggable状态
            )
        }
        
        return null
    }
    
    /**
     * 读取文件内容
     * 
     * 策略:
     * 1. 首先尝试直接使用 `adb shell cat [path]`
     * 2. 如果失败(权限被拒绝),且应用是debuggable,则使用 `run-as` 命令
     * 3. 如果都失败,抛出异常
     * 
     * @param path 文件路径
     * @return 文件内容字符串
     */
    override suspend fun readFileContent(path: String): String {
        // 尝试方法1: 直接访问
        val directResult = try {
            "adb shell cat \"$path\" 2>&1".oneshotShell { output ->
                if (output.contains("Permission denied") || output.contains("No such file")) {
                    null // 权限被拒绝或文件不存在
                } else {
                    output
                }
            }
        } catch (e: Exception) {
            null
        }
        
        if (directResult != null) {
            return directResult
        }
        
        // 方法1失败,尝试方法2: 使用run-as (仅对debuggable应用有效)
        val packageName = cachedPackageName
        if (packageName != null && isDebuggable) {
            return try {
                "adb shell run-as $packageName cat \"$path\" 2>&1".oneshotShell { output ->
                    if (output.contains("Permission denied") || 
                        output.contains("Package") && output.contains("is not debuggable")) {
                        throw Exception("应用不是debuggable模式或权限被拒绝")
                    }
                    output
                }
            } catch (e: Exception) {
                throw Exception("无法读取文件: ${e.message}")
            }
        }
        
        throw Exception("无法读取文件,可能是权限限制。请确保应用是debuggable模式。")
    }
}
