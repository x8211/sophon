package sophon.desktop.feature.activitystack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.activitystack.data.repository.ActivityStackRepositoryImpl
import sophon.desktop.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.activitystack.domain.usecase.GetActivityStackUseCase

class ActivityStackViewModel(
    private val getActivityStackUseCase: GetActivityStackUseCase = GetActivityStackUseCase(
        ActivityStackRepositoryImpl()
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<LifecycleComponent>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private var monitorJob: Job? = null

    /**
     * 开始监测
     */
    fun startMonitoring() {
        // 取消之前的监测任务
        monitorJob?.cancel()

        // 启动新的监测任务
        monitorJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _uiState.value = getActivityStackUseCase()
                } catch (_: Exception) {
                    _uiState.value = emptyList()
                }

                // 每2秒刷新一次
                delay(2000)
            }
        }
    }

    /**
     * 停止监测
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
