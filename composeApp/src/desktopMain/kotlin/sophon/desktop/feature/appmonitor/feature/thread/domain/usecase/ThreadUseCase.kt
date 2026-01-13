package sophon.desktop.feature.appmonitor.feature.thread.domain.usecase

import sophon.desktop.feature.appmonitor.feature.thread.domain.model.ProcessInfo
import sophon.desktop.feature.appmonitor.feature.thread.domain.repository.ThreadRepository

/**
 * 线程信息用例
 * 
 * 根据包名或PID获取进程和线程信息
 */
class ThreadUseCase(private val repository: ThreadRepository) {

    /**
     * 根据包名获取进程信息
     * 
     * @param packageName 应用包名
     * @return 进程信息，如果获取失败返回null
     */
    suspend fun getProcessByPackageName(packageName: String): ProcessInfo? {
        val pid = repository.getPidByPackageName(packageName)
        if (pid.isBlank()) return null
        return repository.getThreadList(pid.trim(), packageName)
    }

    /**
     * 根据PID获取进程信息
     * 
     * @param pid 进程ID
     * @return 进程信息，如果获取失败返回null
     */
    suspend fun getProcessByPid(pid: String): ProcessInfo? {
        if (pid.isBlank()) return null
        return repository.getThreadList(pid.trim())
    }
}
