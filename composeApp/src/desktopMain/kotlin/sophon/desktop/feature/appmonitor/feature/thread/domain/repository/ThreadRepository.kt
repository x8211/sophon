package sophon.desktop.feature.appmonitor.feature.thread.domain.repository

import sophon.desktop.feature.appmonitor.feature.thread.domain.model.ProcessInfo

/**
 * 线程信息仓库接口
 * 
 * 定义获取进程和线程信息的方法
 */
interface ThreadRepository {
    /**
     * 根据包名获取进程ID
     * 
     * @param packageName 应用包名
     * @return 进程ID
     */
    suspend fun getPidByPackageName(packageName: String): String
    
    /**
     * 获取线程列表
     * 
     * @param pid 进程ID
     * @param packageName 应用包名（可选）
     * @return 进程信息，如果获取失败返回null
     */
    suspend fun getThreadList(pid: String, packageName: String = ""): ProcessInfo?
}
