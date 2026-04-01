package sophon.desktop.feature.proxy.domain.usecase

import sophon.desktop.feature.proxy.data.repository.ProxyRepository
import sophon.desktop.feature.proxy.model.ProxyInfo

/**
 * 获取当前代理综合信息
 *
 * 聚合两次 Repository 调用（当前代理地址 + 本机可用 IP 列表），
 * 并计算 proxyEnabled 状态（去除端口后与可用 IP 列表比对）。
 */
class GetProxyInfoUseCase(private val repository: ProxyRepository) {

    suspend operator fun invoke(): ProxyInfo {
        val current = repository.getProxy()
        val options = repository.getLocalIPAddresses()
        val currentIp = current.removeSuffix(":8888")
        return ProxyInfo(
            current = current,
            options = options,
            proxyEnabled = options.isNotEmpty() && options.contains(currentIp)
        )
    }
}
