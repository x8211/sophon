package sophon.desktop.feature.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import java.net.NetworkInterface

class ProxyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyInfo())
    val uiState = _uiState.asStateFlow()

    private val dataSource = ProxyDataSource()

    init {
        viewModelScope.launch {
            Context.stream.collect {
                _uiState.update { ProxyInfo(dataSource.getProxy(), getIPAddress()) }
            }
        }
    }

    fun setProxy(proxy: String) {
        viewModelScope.launch {
            dataSource.modifyProxy(proxy)
            delay(100)
            _uiState.update { it.copy(current = dataSource.getProxy()) }
        }
    }

    fun resetProxy() {
        viewModelScope.launch {
            dataSource.resetProxy()
            delay(100)
            _uiState.update { it.copy(current = dataSource.getProxy()) }
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