package sophon.desktop.feature.proxy

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.NetworkInterface

class ProxyViewModel : StateScreenModel<ProxyInfo>(ProxyInfo()) {

    private val dataSource = ProxyDataSource()

    init {
        screenModelScope.launch {
            Context.stream.collect{
                mutableState.update { ProxyInfo(dataSource.getProxy(), getIPAddress()) }
            }
        }
    }

    fun setProxy(proxy: String) {
        screenModelScope.launch {
            dataSource.modifyProxy(proxy)
            delay(100)
            mutableState.update { it.copy(current = dataSource.getProxy()) }
        }
    }

    fun resetProxy() {
        screenModelScope.launch {
            dataSource.resetProxy()
            delay(100)
            mutableState.update { it.copy(current = dataSource.getProxy()) }
        }
    }
}

/**
 * 获取本机IP地址列表
 */
private fun getIPAddress(): List<String> {
    return NetworkInterface.getNetworkInterfaces()
        .toList()
        .filterNot { it.isLoopback }
        .flatMap { ni -> ni.inetAddresses().filter { it.isSiteLocalAddress }.toList() }
        .map { it.hostAddress }
}