package sophon.desktop.feature.deeplink

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

class DeepLinkViewModel : StateScreenModel<String>("") {

    val history: StateFlow<List<String>> = deepLinkDataStore.data
        .map { it.links }
        // SharingStarted.WhileSubscribed(5000) allows the flow to be kept alive briefly during config changes
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openDeepLink(uri: String) {
        if (uri.isBlank()) {
            mutableState.update { "请输入有效的Deep Link URI" }
            return
        }

        saveToHistory(uri)

        screenModelScope.launch {
            val result = StringBuilder()
            // am start -W -a android.intent.action.VIEW -d <URI>
            "adb shell am start -W -a android.intent.action.VIEW -d \"$uri\"".streamShell()
                .onStart { mutableState.update { "正在打开: $uri" } }
                .onEach { str -> result.appendLine(str) }
                .onCompletion { _ -> mutableState.update { result.toString() } }
                .collect()
        }
    }

    private fun saveToHistory(uri: String) {
        screenModelScope.launch {
            deepLinkDataStore.updateData { current ->
                // Add to top, remove duplicates, limit to 50
                val newLinks = (listOf(uri) + current.links).distinct().take(50)
                current.copy(links = newLinks)
            }
        }
    }

    fun deleteHistory(uri: String) {
        screenModelScope.launch {
            deepLinkDataStore.updateData { current ->
                current.copy(links = current.links - uri)
            }
        }
    }

    fun clearOutput() {
        mutableState.update { "" }
    }
}
