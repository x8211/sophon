package sophon.desktop.feature.thread.domain.repository

import sophon.desktop.feature.thread.domain.model.ProcessInfo

interface ThreadRepository {
    suspend fun getPidByPackageName(packageName: String): String
    suspend fun getThreadList(pid: String, packageName: String = ""): ProcessInfo?
    suspend fun getForegroundPackageName(): String
}
