package sophon.desktop.feature.proxy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.feature.proxy.data.repository.ProxyRepositoryImpl
import sophon.desktop.feature.proxy.domain.usecase.GetProxyInfoUseCase
import sophon.desktop.feature.proxy.model.ProxyInfo

class ProxyViewModel(
    private val repository: ProxyRepositoryImpl = ProxyRepositoryImpl(),
    private val getProxyInfoUseCase: GetProxyInfoUseCase = GetProxyInfoUseCase(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyInfo())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            Context.stream.collect {
                _uiState.value = getProxyInfoUseCase()
            }
        }
    }

    fun setProxy(proxy: String) {
        viewModelScope.launch {
            repository.modifyProxy(proxy)
            delay(100)
            _uiState.update { it.copy(current = repository.getProxy(), proxyEnabled = true) }
        }
    }

    fun resetProxy() {
        viewModelScope.launch {
            repository.resetProxy()
            delay(100)
            _uiState.update { it.copy(current = repository.getProxy(), proxyEnabled = false) }
        }
    }
}
