package sophon.desktop.feature.installapk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.streamShell

class InstallApkViewModel : ViewModel() {

    private val _uiState = MutableStateFlow("")
    val uiState = _uiState.asStateFlow()

    fun installApk(apkPath: String?) {
        viewModelScope.launch {
            apkPath?.takeIf { it.endsWith(".apk") }?.apply {
                val result = StringBuilder()
                "adb install $apkPath".streamShell()
                    .onStart { _uiState.update { "安装中，注意手机弹窗" } }
                    .onEach { str -> result.appendLine(str) }
                    .onCompletion { _ -> _uiState.update { result.toString() } }
                    .collect()
            } ?: _uiState.update { "文件错误：${apkPath}" }
        }
    }

}