package sophon.desktop.feature.deeplink.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sophon.desktop.core.Shell.streamShell
import sophon.desktop.core.datastore.DataStoreProvider

class DeepLinkRepositoryImpl : DeepLinkRepository {

    override fun executeDeepLink(uri: String): Flow<String> {
        // am start -W -a android.intent.action.VIEW -d <URI>
        return "adb shell am start -W -a android.intent.action.VIEW -d \"$uri\"".streamShell()
    }

    override fun getHistory(): Flow<List<String>> {
        return DataStoreProvider.deepLinkHistory.data.map { it.links }
    }

    override suspend fun saveHistory(uri: String) {
        DataStoreProvider.deepLinkHistory.updateData { current ->
            val newLinks = (listOf(uri) + current.links).distinct().take(50)
            current.copy(links = newLinks)
        }
    }

    override suspend fun deleteHistory(uri: String) {
        DataStoreProvider.deepLinkHistory.updateData { current ->
            current.copy(links = current.links - uri)
        }
    }
}
