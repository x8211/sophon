package sophon.desktop.feature.deeplink.data.source

import kotlinx.serialization.Serializable
import sophon.desktop.core.datastore.createDataStore

@Serializable
data class DeepLinkHistoryModel(
    val links: List<String> = emptyList()
)

val deepLinkDataStore = createDataStore(
    fileName = "deepLink.pb",
    defaultValue = DeepLinkHistoryModel(),
    serializer = DeepLinkHistoryModel.serializer()
)
