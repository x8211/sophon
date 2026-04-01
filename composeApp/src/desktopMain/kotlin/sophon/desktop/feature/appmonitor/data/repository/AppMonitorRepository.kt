package sophon.desktop.feature.appmonitor.data.repository

import sophon.desktop.feature.appmonitor.model.AppInfo

/**
 * 应用监控仓库接口
 * 
 * 定义获取当前前台应用信息的方法
 */
interface AppMonitorRepository {
    
    /**
     * 获取当前前台应用信息
     * 
     * @return 应用信息，如果获取失败返回null
     */
    suspend fun getForegroundAppInfo(): AppInfo?
}
