package sophon.desktop.feature.appmonitor.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.appmonitor.domain.model.AppInfo
import sophon.desktop.feature.appmonitor.domain.repository.AppMonitorRepository

/**
 * 应用监控仓库实现
 * 
 * 使用ADB命令获取当前前台应用的包名和debuggable状态
 */
class AppMonitorRepositoryImpl : AppMonitorRepository {
    
    /**
     * 获取当前前台应用信息
     * 
     * 通过以下步骤获取：
     * 1. 使用 dumpsys activity activities 获取前台应用包名
     * 2. 使用 dumpsys package [packageName] 获取应用的debuggable状态
     * 
     * @return 应用信息，如果获取失败返回null
     */
    override suspend fun getForegroundAppInfo(): AppInfo? {
        // 步骤1: 获取前台应用包名
        val packageName = getForegroundPackageName()
        if (packageName.isBlank()) {
            return null
        }
        
        // 步骤2: 获取debuggable状态
        val isDebuggable = checkIsDebuggable(packageName)
        
        return AppInfo(
            packageName = packageName,
            isDebuggable = isDebuggable
        )
    }
    
    /**
     * 获取当前前台应用包名
     * 
     * 使用 dumpsys activity activities 命令，从输出中提取包名
     * 
     * @return 包名字符串，如果获取失败返回空字符串
     */
    private suspend fun getForegroundPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { output ->
                // 匹配格式: * Task{12345:com.example.app/...}
                "A=\\d+:(\\S+)".toRegex().find(output)?.groupValues?.getOrNull(1) ?: ""
            }
    }
    
    /**
     * 检查应用是否为debuggable模式
     * 
     * 使用 dumpsys package [packageName] 命令，检查flags字段
     * 
     * @param packageName 应用包名
     * @return 是否为debuggable模式
     */
    private suspend fun checkIsDebuggable(packageName: String): Boolean {
        return "adb shell dumpsys package $packageName".oneshotShell { output ->
            // 检查是否包含DEBUGGABLE标志
            if (output.contains("flags=") && output.contains("DEBUGGABLE")) {
                return@oneshotShell true
            }
            
            // 检查十六进制flags值
            checkDebuggableFlag(output)
        }
    }
    
    /**
     * 检查应用的flags字段是否包含debuggable标志
     * 
     * flags字段示例: 
     * - flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]
     * - flags=0x82 (bit 1表示debuggable)
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
}
