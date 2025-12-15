package sophon.desktop.feature.taskrecord

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.oneshotShell

class TaskRecordViewModel : StateScreenModel<List<LifecycleComponent>>(emptyList()) {

    init {
        screenModelScope.launch {
            Context.stream.collect {
                val detail = queryDetail(queryPackageName())
                mutableState.value = detail
            }
        }
    }


    private suspend fun queryPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }

    private suspend fun queryDetail(packageName: String): List<LifecycleComponent> {
        packageName.ifBlank { return emptyList() }
        return "adb shell dumpsys activity $packageName".oneshotShell(ActivityTaskParser::parse)
    }

}