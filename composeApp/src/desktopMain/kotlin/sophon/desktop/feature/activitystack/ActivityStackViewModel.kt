package sophon.desktop.feature.activitystack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.oneshotShell

class ActivityStackViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<List<LifecycleComponent>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _ticker = MutableSharedFlow<Long>()

    init {
        viewModelScope.launch {
            while (true) {
                _ticker.emit(System.currentTimeMillis())
                delay(2000)
            }
        }
        viewModelScope.launch {
            combine(Context.stream, _ticker) { context, _ -> context }
                .collect {
                    val detail = queryDetail(queryPackageName())
                    _uiState.value = detail
                }
        }
    }


    private suspend fun queryPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }

    private suspend fun queryDetail(packageName: String): List<LifecycleComponent> {
        packageName.ifBlank { return emptyList() }
        return "adb shell dumpsys activity $packageName".oneshotShell(ActivityStackParser::parse)
    }

}