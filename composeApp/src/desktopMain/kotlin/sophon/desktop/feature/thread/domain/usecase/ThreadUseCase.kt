package sophon.desktop.feature.thread.domain.usecase

import sophon.desktop.feature.thread.domain.model.ThreadInfo
import sophon.desktop.feature.thread.domain.repository.ThreadRepository

class ThreadUseCase(private val repository: ThreadRepository) {

    suspend fun getThreadsByPackageName(packageName: String): List<ThreadInfo> {
        val pid = repository.getPidByPackageName(packageName)
        if (pid.isBlank()) return emptyList()
        return repository.getThreadList(pid.trim())
    }

    suspend fun getThreadsByPid(pid: String): List<ThreadInfo> {
        if (pid.isBlank()) return emptyList()
        return repository.getThreadList(pid.trim())
    }

    suspend fun getThreadsForForegroundApp(): List<ThreadInfo> {
        val packageName = repository.getForegroundPackageName()
        if (packageName.isBlank()) return emptyList()
        val pid = repository.getPidByPackageName(packageName)
        if (pid.isBlank()) return emptyList()
        return repository.getThreadList(pid.trim())
    }
}
