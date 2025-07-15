package sophon.desktop.feature.installapk

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Shell.streamShell
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InstallApkViewModel : StateScreenModel<String>("") {

    fun installApk(apkPath: String?) {
        screenModelScope.launch {
            apkPath?.takeIf { it.endsWith(".apk") }?.apply {
                val result = StringBuilder()
                "adb install $apkPath".streamShell()
                    .onStart { mutableState.update { "安装中，注意手机弹窗" } }
                    .onEach { str -> result.appendLine(str) }
                    .onCompletion { _ -> mutableState.update { result.toString() } }
                    .collect()
            } ?: mutableState.update { "文件错误：${apkPath}" }
        }
    }

}