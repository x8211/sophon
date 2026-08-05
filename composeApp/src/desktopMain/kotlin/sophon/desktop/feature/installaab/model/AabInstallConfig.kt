package sophon.desktop.feature.installaab.model

data class AabInstallConfig(
    val aabPath: String,
    val keystorePath: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)
