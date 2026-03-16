package sophon.desktop.feature.proxy.domain.repository

interface ProxyRepository {
    suspend fun getProxy(): String
    suspend fun modifyProxy(proxy: String)
    suspend fun resetProxy()
    fun getLocalIPAddresses(): List<String>
}
