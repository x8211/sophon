package sophon.desktop.feature.thread.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.thread.data.repository.ThreadRepositoryImpl
import sophon.desktop.feature.thread.domain.model.ProcessInfo
import sophon.desktop.feature.thread.domain.usecase.ThreadUseCase

class ThreadViewModel(
    private val useCase: ThreadUseCase = ThreadUseCase(ThreadRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessInfo?>(null)
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
                    _uiState.value = useCase.getProcessForForegroundApp()
                    println("ThreadViewModel: $isActive")
                } catch (_: Exception) {
                    _uiState.value = null
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
