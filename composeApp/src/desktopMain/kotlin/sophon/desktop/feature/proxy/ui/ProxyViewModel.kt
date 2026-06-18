package sophon.desktop.feature.proxy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.feature.proxy.data.repository.ProxyRepositoryImpl
import sophon.desktop.feature.proxy.model.ProxyInfo

class ProxyViewModel(
    private val repository: ProxyRepositoryImpl = ProxyRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyInfo())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            Context.stream.collect {
                _uiState.value = buildProxyInfo()
            }
        }
    }

    fun setProxy(proxy: String) {
        viewModelScope.launch {
            repository.modifyProxy(proxy)
            delay(100)
            _uiState.value = buildProxyInfo()
        }
    }

    fun resetProxy() {
        viewModelScope.launch {
            repository.resetProxy()
            delay(100)
            _uiState.value = buildProxyInfo()
        }
    }

    private suspend fun buildProxyInfo(): ProxyInfo {
        val current = repository.getProxy()
        val options = repository.getLocalIPAddresses()
        val currentIp = current.removeSuffix(":8888")
        return ProxyInfo(
            current = current,
            options = options,
            proxyEnabled = options.isNotEmpty() && options.any { it.ipAddress == currentIp }
        )
    }
}
