package sophon.desktop.feature.thread

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.oneshotShell

class ThreadViewModel : StateScreenModel<List<ThreadInfo>>(emptyList()) {

    private val threadDataSource = ThreadDataSource()

    init {
        screenModelScope.launch {
            Context.stream.collect {
                val pid = threadDataSource.queryPidWithPkg(queryPackageName())
                val threadInfo = threadDataSource.queryThreadList(pid)
                mutableState.value = threadInfo
            }
        }
    }

    private suspend fun queryPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }

}