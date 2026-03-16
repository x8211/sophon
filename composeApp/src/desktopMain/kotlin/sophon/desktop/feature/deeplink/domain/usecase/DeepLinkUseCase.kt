package sophon.desktop.feature.deeplink.domain.usecase

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.deeplink.domain.repository.DeepLinkRepository

/**
 * DeepLink 业务逻辑封装
 */
class DeepLinkUseCase(private val repository: DeepLinkRepository) {

    fun execute(uri: String): Flow<String> = repository.executeDeepLink(uri)

    fun getHistory(): Flow<List<String>> = repository.getHistory()

    suspend fun saveHistory(uri: String) = repository.saveHistory(uri)

    suspend fun deleteHistory(uri: String) = repository.deleteHistory(uri)
}
