package sophon.desktop.feature.update.data.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.update.model.UpdateInfo
import java.io.File

interface UpdateRepository {
    /**
     * 检查是否有新版本。
     * @return 若有新版本则返回 [UpdateInfo]，已是最新版则返回 null。
     */
    suspend fun checkForUpdate(): UpdateInfo?

    /**
     * 后台下载安装包，通过 [Flow] 上报进度。
     * @param info 包含平台对应下载链接的版本信息。
     */
    fun downloadUpdate(info: UpdateInfo): Flow<DownloadState>

    /**
     * 清理已下载的更新安装包，释放磁盘空间。
     * 删除失败时静默忽略，不打断调用方。
     */
    suspend fun cleanupDownloadedUpdates()
}

sealed class DownloadState {
    data class Progress(val progress: Float) : DownloadState()
    data class Complete(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
