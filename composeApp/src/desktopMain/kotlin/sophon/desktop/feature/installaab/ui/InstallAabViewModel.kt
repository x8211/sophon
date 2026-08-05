package sophon.desktop.feature.installaab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.feature.installaab.data.repository.InstallAabRepositoryImpl
import sophon.desktop.feature.installaab.data.source.AabKeystoreCache
import sophon.desktop.feature.installaab.model.AabInstallConfig

data class InstallAabUiState(
    val aabPath: String = "",
    val isKeystoreExpanded: Boolean = true,
    val keystorePath: String = "",
    val storePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = "",
    val output: String = "",
    val isInstalling: Boolean = false,
)

class InstallAabViewModel(
    private val repository: InstallAabRepositoryImpl = InstallAabRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallAabUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cache = repository.getKeystoreCache().first()
            _uiState.update {
                it.copy(
                    keystorePath = cache.keystorePath,
                    storePassword = cache.storePassword,
                    keyAlias = cache.keyAlias,
                    keyPassword = cache.keyPassword,
                )
            }
        }
    }

    fun onAabPathSelected(path: String?) {
        if (path == null || !path.endsWith(".aab")) {
            if (path != null) _uiState.update { it.copy(output = "文件错误：仅支持 .aab 文件\n$path") }
            return
        }
        _uiState.update { it.copy(aabPath = path) }
    }

    fun onKeystoreExpandedToggle() =
        _uiState.update { it.copy(isKeystoreExpanded = !it.isKeystoreExpanded) }

    fun onKeystorePathSelected(path: String?) {
        if (path != null) {
            _uiState.update { it.copy(keystorePath = path) }
            saveKeystoreCache()
        }
    }

    fun onStorePasswordChange(value: String) {
        _uiState.update { it.copy(storePassword = value) }
        saveKeystoreCache()
    }

    fun onKeyAliasChange(value: String) {
        _uiState.update { it.copy(keyAlias = value) }
        saveKeystoreCache()
    }

    fun onKeyPasswordChange(value: String) {
        _uiState.update { it.copy(keyPassword = value) }
        saveKeystoreCache()
    }

    fun clearOutput() = _uiState.update { it.copy(output = "") }

    fun installAab() {
        val state = _uiState.value
        if (state.aabPath.isBlank()) return

        val config = AabInstallConfig(
            aabPath = state.aabPath,
            keystorePath = state.keystorePath,
            storePassword = state.storePassword,
            keyAlias = state.keyAlias,
            keyPassword = state.keyPassword,
        )

        viewModelScope.launch {
            val result = StringBuilder()
            repository.installAab(config)
                .onStart { _uiState.update { it.copy(isInstalling = true, output = "") } }
                .onEach { line -> result.append(line) }
                .onCompletion { _uiState.update { it.copy(isInstalling = false, output = result.toString()) } }
                .collect()
        }
    }

    private fun saveKeystoreCache() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveKeystoreCache(
                AabKeystoreCache(
                    keystorePath = state.keystorePath,
                    storePassword = state.storePassword,
                    keyAlias = state.keyAlias,
                    keyPassword = state.keyPassword,
                )
            )
        }
    }
}
