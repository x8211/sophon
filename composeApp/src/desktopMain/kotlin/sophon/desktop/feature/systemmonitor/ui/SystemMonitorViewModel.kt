package sophon.desktop.feature.systemmonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.systemmonitor.data.repository.SystemMonitorRepositoryImpl
import sophon.desktop.feature.systemmonitor.domain.usecase.GetSystemInfoUseCase

/**
 * 系统监控ViewModel
 *
 * 管理系统监控页面的状态，包括系统信息和子功能选择
 * 使用2秒轮询机制持续刷新系统信息
 */
class SystemMonitorViewModel : ViewModel() {

    private val repository = SystemMonitorRepositoryImpl()
    private val getSystemInfoUseCase = GetSystemInfoUseCase(repository)

    // 当前选中的子功能
    private val _selectedFeature = MutableStateFlow(SystemMonitorFeature.TEMPERATURE)
    val selectedFeature = _selectedFeature.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // 刷新触发器，值为当前时间戳
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    // 轮询任务
    private var pollingJob: Job? = null

    /**
     * 启动轮询获取系统信息
     * 每2秒轮询一次
     */
    fun startPolling() {
        // 取消之前的轮询任务
        pollingJob?.cancel()

        // 启动新的轮询任务
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _errorMessage.value = null
                    _refreshTrigger.value = getSystemInfoUseCase()
                } catch (e: Exception) {
                    _errorMessage.value = "加载失败: ${e.message}"
                }

                // 每2秒轮询一次（使用可取消的delay）
                delay(2000)
            }
        }
    }

    /**
     * 停止轮询
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * 选择子功能
     *
     * @param feature 要选择的子功能
     */
    fun selectFeature(feature: SystemMonitorFeature) {
        _selectedFeature.value = feature
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
