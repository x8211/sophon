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
import sophon.desktop.feature.proxy.domain.model.ProxyInfo
import sophon.desktop.feature.proxy.domain.usecase.ProxyUseCase

class ProxyViewModel(
    private val useCase: ProxyUseCase = ProxyUseCase(ProxyRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyInfo())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            Context.stream.collect {
                _uiState.value = useCase.getProxyInfo()
            }
        }
    }

    fun setProxy(proxy: String) {
        viewModelScope.launch {
            useCase.setProxy(proxy)
            delay(100)
            _uiState.update { it.copy(current = useCase.getCurrentProxy(), proxyEnabled = true) }
        }
    }

    fun resetProxy() {
        viewModelScope.launch {
            useCase.resetProxy()
            delay(100)
            _uiState.update { it.copy(current = useCase.getCurrentProxy(), proxyEnabled = false) }
        }
    }
}
