package sophon.desktop.feature.systemmonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.gfxmonitor.data.repository.GfxMonitorRepositoryImpl
import sophon.desktop.feature.systemmonitor.data.repository.SystemMonitorRepositoryImpl
import sophon.desktop.feature.systemmonitor.domain.model.SystemMonitorData
import sophon.desktop.feature.systemmonitor.domain.usecase.GetSystemMonitorDataUseCase

/**
 * 系统监测 ViewModel
 * 负责定时获取系统监测数据并更新UI状态
 */
class SystemMonitorViewModel(
    private val getSystemMonitorDataUseCase: GetSystemMonitorDataUseCase =
        GetSystemMonitorDataUseCase(SystemMonitorRepositoryImpl(), GfxMonitorRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<SystemMonitorData>(SystemMonitorData())
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 监测项可见性状态 (默认全部开启)
    private val _visibleMonitors = MutableStateFlow(setOf("Temperature", "Display"))
    val visibleMonitors = _visibleMonitors.asStateFlow()

    init {
        startMonitoring()
    }

    /**
     * 切换监测项可见性
     */
    fun toggleMonitorVisibility(monitor: String) {
        val current = _visibleMonitors.value.toMutableSet()
        if (current.contains(monitor)) {
            current.remove(monitor)
        } else {
            current.add(monitor)
        }
        _visibleMonitors.value = current
    }

    /**
     * 开始监测,每2秒更新一次数据
     */
    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                val monitors = _visibleMonitors.value
                if (monitors.isNotEmpty()) {
                    try {
                        _isLoading.value = true
                        val data = getSystemMonitorDataUseCase(monitors)
                        _uiState.value = data
                    } catch (e: Exception) {
                        // 仅在数据获取失败时记录
                        e.printStackTrace()
                    } finally {
                        _isLoading.value = false
                    }
                } else {
                    _uiState.value = SystemMonitorData()
                }
                delay(2000) // 每2秒更新一次
            }
        }
    }
}
