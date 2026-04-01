package sophon.desktop.feature.appmonitor.feature.thread.domain.usecase

import sophon.desktop.feature.appmonitor.feature.thread.data.repository.ThreadRepository
import sophon.desktop.feature.appmonitor.feature.thread.model.ProcessInfo

/**
 * 根据包名或PID获取进程和线程信息
 *
 * 编排多步 Repository 调用：先解析 PID，再获取线程列表，并包含空值守卫与字符串规范化逻辑。
 */
class GetThreadInfoUseCase(private val repository: ThreadRepository) {

    /**
     * 根据包名获取进程信息
     *
     * @param packageName 应用包名
     * @return 进程信息，如果获取失败返回null
     */
    suspend fun byPackageName(packageName: String): ProcessInfo? {
        val pid = repository.getPidByPackageName(packageName)
        if (pid.isBlank()) return null
        return repository.getThreadList(pid.trim(), packageName)
    }
}
