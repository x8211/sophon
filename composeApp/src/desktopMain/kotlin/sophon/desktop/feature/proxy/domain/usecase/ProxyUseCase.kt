package sophon.desktop.feature.proxy.domain.usecase

import sophon.desktop.feature.proxy.domain.model.ProxyInfo
import sophon.desktop.feature.proxy.domain.repository.ProxyRepository

class ProxyUseCase(private val repository: ProxyRepository) {

    suspend fun getProxyInfo(): ProxyInfo {
        val current = repository.getProxy()
        val options = repository.getLocalIPAddresses()
        val currentIp = current.removeSuffix(":8888")
        return ProxyInfo(
            current,
            options,
            proxyEnabled = options.isNotEmpty() && options.contains(currentIp)
        )
    }

    suspend fun setProxy(proxy: String) {
        repository.modifyProxy(proxy)
    }

    suspend fun resetProxy() {
        repository.resetProxy()
    }

    suspend fun getCurrentProxy(): String {
        return repository.getProxy()
    }
}
