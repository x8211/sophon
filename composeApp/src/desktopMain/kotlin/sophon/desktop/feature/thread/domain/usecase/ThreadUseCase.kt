package sophon.desktop.feature.thread.domain.usecase

import sophon.desktop.feature.thread.domain.model.ProcessInfo
import sophon.desktop.feature.thread.domain.repository.ThreadRepository

class ThreadUseCase(private val repository: ThreadRepository) {

    suspend fun getProcessByPackageName(packageName: String): ProcessInfo? {
        val pid = repository.getPidByPackageName(packageName)
        if (pid.isBlank()) return null
        return repository.getThreadList(pid.trim(), packageName)
    }

    suspend fun getProcessByPid(pid: String): ProcessInfo? {
        if (pid.isBlank()) return null
        return repository.getThreadList(pid.trim())
    }

    suspend fun getProcessForForegroundApp(): ProcessInfo? {
        val packageName = repository.getForegroundPackageName()
        if (packageName.isBlank()) return null
        val pid = repository.getPidByPackageName(packageName)
        if (pid.isBlank()) return null
        return repository.getThreadList(pid.trim(), packageName)
    }
}
