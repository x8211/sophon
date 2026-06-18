package sophon.desktop.feature.proxy.model

/**
 * [name] 网络接口名称，如 en0、utun4
 * [ipAddress] 对应的 IP 地址
 */
data class LocalNetworkInterface(
    val name: String,
    val ipAddress: String,
)
