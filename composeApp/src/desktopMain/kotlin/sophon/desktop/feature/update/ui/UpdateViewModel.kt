package sophon.desktop.feature.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sophon.desktop.core.datastore.DataStoreProvider
import sophon.desktop.feature.update.data.repository.DownloadState
import sophon.desktop.feature.update.data.repository.UpdateRepositoryImpl
import sophon.desktop.feature.update.model.UpdateInfo
import java.awt.Desktop
import java.io.File

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class NewVersion(val info: UpdateInfo) : UpdateUiState()
    /** 正在下载，[progress] 范围 0.0–1.0，-1f 表示进度未知。 */
    data class Downloading(val progress: Float) : UpdateUiState()
    data class ReadyToInstall(val file: File) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

class UpdateViewModel : ViewModel() {

    private val repository = UpdateRepositoryImpl()

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 延迟 10 秒后自动检查，避免干扰应用启动
            delay(10_000)
            checkForUpdate()
        }
    }

    /** 手动或自动触发版本检查。 */
    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            try {
                val info = repository.checkForUpdate()
                if (info != null) {
                    val ignoredVersion = DataStoreProvider.updatePrefs.data.first().ignoredVersion
                    _uiState.value = if (info.version == ignoredVersion) {
                        UpdateUiState.UpToDate
                    } else {
                        UpdateUiState.NewVersion(info)
                    }
                } else {
                    _uiState.value = UpdateUiState.UpToDate
                }
            } catch (e: Exception) {
                _uiState.value = UpdateUiState.Error(e.message ?: "检查更新失败")
            }
        }
    }

    /** 开始后台下载并在完成后自动打开安装程序。 */
    fun downloadAndInstall(info: UpdateInfo) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Downloading(-1f)
            repository.downloadUpdate(info).collect { state ->
                when (state) {
                    is DownloadState.Progress -> _uiState.value = UpdateUiState.Downloading(state.progress)
                    is DownloadState.Complete -> {
                        _uiState.value = UpdateUiState.ReadyToInstall(state.file)
                        openInstaller(state.file)
                    }
                    is DownloadState.Error -> _uiState.value = UpdateUiState.Error(state.message)
                }
            }
        }
    }

    /**
     * 忽略指定版本，下次启动不再提示该版本。
     * 仅当发布更高版本时，提示才会再次出现。
     */
    fun ignoreVersion(version: String) {
        viewModelScope.launch {
            DataStoreProvider.updatePrefs.updateData { it.copy(ignoredVersion = version) }
            _uiState.value = UpdateUiState.Idle
        }
    }

    /** 关闭 Banner（本次会话内不再显示，下次启动仍会检查）。 */
    fun dismiss() {
        _uiState.value = UpdateUiState.Idle
    }

    private suspend fun openInstaller(file: File) = withContext(Dispatchers.Main) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
    }
}
