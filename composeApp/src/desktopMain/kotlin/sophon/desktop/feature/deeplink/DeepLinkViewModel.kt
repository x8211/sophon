package sophon.desktop.feature.deeplink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.streamShell
import sophon.desktop.datastore.deepLinkDataStore

class DeepLinkViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<String>("")
    val uiState = _uiState.asStateFlow()

    val history: StateFlow<List<String>> = deepLinkDataStore.data
        .map { it.links }
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
            // am start -W -a android.intent.action.VIEW -d <URI>
            "adb shell am start -W -a android.intent.action.VIEW -d \"$uri\"".streamShell()
                .onStart { _uiState.update { "正在打开: $uri" } }
                .onEach { str -> result.appendLine(str) }
                .onCompletion { _ -> _uiState.update { result.toString() } }
                .collect()
        }
    }

    private fun saveToHistory(uri: String) {
        viewModelScope.launch {
            deepLinkDataStore.updateData { current ->
                // Add to top, remove duplicates, limit to 50
                val newLinks = (listOf(uri) + current.links).distinct().take(50)
                current.copy(links = newLinks)
            }
        }
    }

    fun deleteHistory(uri: String) {
        viewModelScope.launch {
            deepLinkDataStore.updateData { current ->
                current.copy(links = current.links - uri)
            }
        }
    }

    fun clearOutput() {
        _uiState.update { "" }
    }
}
