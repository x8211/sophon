package sophon.desktop.feature.deeplink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.feature.deeplink.data.repository.DeepLinkRepositoryImpl
import sophon.desktop.feature.deeplink.domain.usecase.DeepLinkUseCase

class DeepLinkViewModel(
    private val deepLinkUseCase: DeepLinkUseCase = DeepLinkUseCase(DeepLinkRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<String>("")
    val uiState = _uiState.asStateFlow()

    val history: StateFlow<List<String>> = deepLinkUseCase.getHistory()
        // SharingStarted.WhileSubscribed(5000) allows the flow to be kept alive briefly during config changes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openDeepLink(uri: String) {
        if (uri.isBlank()) {
            _uiState.update { "请输入有效的Deep Link URI" }
            return
        }

        saveToHistory(uri)

        viewModelScope.launch {
            val result = StringBuilder()
            deepLinkUseCase.execute(uri)
                .onStart { _uiState.update { "正在打开: $uri" } }
                .onEach { str -> result.appendLine(str) }
                .onCompletion { _ -> _uiState.update { result.toString() } }
                .collect()
        }
    }

    private fun saveToHistory(uri: String) {
        viewModelScope.launch {
            deepLinkUseCase.saveHistory(uri)
        }
    }

    fun deleteHistory(uri: String) {
        viewModelScope.launch {
            deepLinkUseCase.deleteHistory(uri)
        }
    }

    fun clearOutput() {
        _uiState.update { "" }
    }
}
