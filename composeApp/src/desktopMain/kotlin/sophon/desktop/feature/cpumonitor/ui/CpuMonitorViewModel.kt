package sophon.desktop.feature.cpumonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.cpumonitor.data.repository.CpuMonitorRepositoryImpl
import sophon.desktop.feature.cpumonitor.domain.model.CpuMonitorData
import sophon.desktop.feature.cpumonitor.domain.usecase.GetCpuMonitorDataUseCase

/**
 * CPU监测ViewModel
 */
class CpuMonitorViewModel : ViewModel() {

    private val repository = CpuMonitorRepositoryImpl()
    private val getCpuMonitorDataUseCase = GetCpuMonitorDataUseCase(repository)

    private val _uiState = MutableStateFlow<CpuMonitorUiState>(CpuMonitorUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var monitorJob: Job? = null
    private var currentPackageName: String? = null

    /**
     * 开始监测
     * @param packageName 包名,如果为null则监测所有进程
     */
    fun startMonitoring(packageName: String? = null) {
        currentPackageName = packageName

        // 取消之前的监测任务
        monitorJob?.cancel()

        // 启动新的监测任务
        monitorJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val data = getCpuMonitorDataUseCase(currentPackageName)
                    _uiState.value = CpuMonitorUiState.Success(data)
                } catch (e: Exception) {
                    _uiState.value = CpuMonitorUiState.Error(e.message ?: "未知错误")
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

    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.value = CpuMonitorUiState.Loading
                val data = getCpuMonitorDataUseCase(currentPackageName)
                _uiState.value = CpuMonitorUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = CpuMonitorUiState.Error(e.message ?: "未知错误")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}

/**
 * CPU监测UI状态
 */
sealed class CpuMonitorUiState {
    data object Loading : CpuMonitorUiState()
    data class Success(val data: CpuMonitorData) : CpuMonitorUiState()
    data class Error(val message: String) : CpuMonitorUiState()
}
