package sophon.desktop.feature.deeplink.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sophon.desktop.core.Shell.streamShell
import sophon.desktop.feature.deeplink.data.source.deepLinkDataStore
import sophon.desktop.feature.deeplink.domain.repository.DeepLinkRepository

class DeepLinkRepositoryImpl : DeepLinkRepository {

    override fun executeDeepLink(uri: String): Flow<String> {
        // am start -W -a android.intent.action.VIEW -d <URI>
        return "adb shell am start -W -a android.intent.action.VIEW -d \"$uri\"".streamShell()
    }

    override fun getHistory(): Flow<List<String>> {
        return deepLinkDataStore.data.map { it.links }
    }

    override suspend fun saveHistory(uri: String) {
        deepLinkDataStore.updateData { current ->
            // Add to top, remove duplicates, limit to 50
            val newLinks = (listOf(uri) + current.links).distinct().take(50)
            current.copy(links = newLinks)
        }
    }

    override suspend fun deleteHistory(uri: String) {
        deepLinkDataStore.updateData { current ->
            current.copy(links = current.links - uri)
        }
    }
}
