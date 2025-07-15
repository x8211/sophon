package sophon.desktop.feature.proxy

/**
 * [current]当前代理
 * [options]本机可选代理
 */
data class ProxyInfo(val current: String = "", val options: List<String> = emptyList())
