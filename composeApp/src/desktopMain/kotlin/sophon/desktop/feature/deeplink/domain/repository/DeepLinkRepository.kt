package sophon.desktop.feature.deeplink.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * DeepLink 仓库接口
 */
interface DeepLinkRepository {
    /**
     * 执行 DeepLink
     * @param uri URI字符串
     * @return 执行过程的输出流
     */
    fun executeDeepLink(uri: String): Flow<String>

    /**
     * 获取历史记录
     */
    fun getHistory(): Flow<List<String>>

    /**
     * 保存 URI 到历史记录
     */
    suspend fun saveHistory(uri: String)

    /**
     * 从历史记录删除 URI
     */
    suspend fun deleteHistory(uri: String)
}
