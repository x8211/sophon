package sophon.desktop.feature.proxy.model

/**
 * [current] 当前代理
 * [options] 本机可选代理（含网络接口名称与 IP）
 */
data class ProxyInfo(
    val current: String = "",
    val options: List<LocalNetworkInterface> = emptyList(),
    val proxyEnabled: Boolean = false
)