package sophon.desktop.feature.deeplink.data.source

import kotlinx.serialization.Serializable

@Serializable
data class DeepLinkHistoryModel(
    val links: List<String> = emptyList()
)
