package sophon.desktop.feature.thread

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Context
import sophon.desktop.core.GlobalTimer
import sophon.desktop.feature.taskrecord.TopTaskDataSource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThreadViewModel : StateScreenModel<List<ThreadInfo>>(emptyList()) {

    private val topTaskDataSource = TopTaskDataSource()
    private val threadDataSource = ThreadDataSource()

    init {
        screenModelScope.launch {
            combine(Context.stream, GlobalTimer.tick) { _, _ ->
                val overview = topTaskDataSource.queryPackageName()
                val pid = threadDataSource.queryPidWithPkg(overview)
                val threadInfo = threadDataSource.queryThreadList(pid)
                mutableState.value = threadInfo
            }.stateIn(this)
        }
    }

}