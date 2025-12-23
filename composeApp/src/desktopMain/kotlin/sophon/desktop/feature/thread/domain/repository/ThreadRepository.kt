package sophon.desktop.feature.thread.domain.repository

import sophon.desktop.feature.thread.domain.model.ThreadInfo

interface ThreadRepository {
    suspend fun getPidByPackageName(packageName: String): String
    suspend fun getThreadList(pid: String): List<ThreadInfo>
    suspend fun getForegroundPackageName(): String
}
