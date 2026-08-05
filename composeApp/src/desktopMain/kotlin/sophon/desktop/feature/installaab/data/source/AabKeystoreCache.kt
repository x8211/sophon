package sophon.desktop.feature.installaab.data.source

import kotlinx.serialization.Serializable

@Serializable
data class AabKeystoreCache(
    val keystorePath: String = "",
    val storePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = "",
)
