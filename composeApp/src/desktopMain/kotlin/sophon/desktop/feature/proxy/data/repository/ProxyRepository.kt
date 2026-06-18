package sophon.desktop.feature.proxy.data.repository

import sophon.desktop.feature.proxy.model.LocalNetworkInterface

interface ProxyRepository {
    suspend fun getProxy(): String
    suspend fun modifyProxy(proxy: String)
    suspend fun resetProxy()
    fun getLocalIPAddresses(): List<LocalNetworkInterface>
}
