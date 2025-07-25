package sophon.desktop.feature.thread

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Context
import sophon.desktop.core.GlobalTimer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.oneshotShell

class ThreadViewModel : StateScreenModel<List<ThreadInfo>>(emptyList()) {

    private val threadDataSource = ThreadDataSource()

    init {
        screenModelScope.launch {
            combine(Context.stream, GlobalTimer.tick) { _, _ ->
                val pid = threadDataSource.queryPidWithPkg(queryPackageName())
                val threadInfo = threadDataSource.queryThreadList(pid)
                mutableState.value = threadInfo
            }.stateIn(this)
        }
    }

    private suspend fun queryPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }

}