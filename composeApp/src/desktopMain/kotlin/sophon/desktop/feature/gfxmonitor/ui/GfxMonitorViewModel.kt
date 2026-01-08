package sophon.desktop.feature.gfxmonitor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.gfxmonitor.data.repository.GfxMonitorRepositoryImpl
import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData
import sophon.desktop.feature.gfxmonitor.domain.usecase.GetGfxDataUseCase

/**
 * 图形监测 ViewModel
 * 负责管理数据加载、定时刷新和状态维护
 */
class GfxMonitorViewModel {
    private val repository = GfxMonitorRepositoryImpl()
    private val getGfxDataUseCase = GetGfxDataUseCase(repository)
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    // 当前显示的图形数据
    var displayData by mutableStateOf<DisplayData?>(null)
        private set

    // 是否正在刷新
    var isRefreshing by mutableStateOf(false)
        private set

    private var refreshJob: Job? = null

    /**
     * 开始定时加载数据
     */
    fun startMonitoring() {
        if (refreshJob?.isActive == true) return
        
        refreshJob = viewModelScope.launch {
            while (isActive) {
                isRefreshing = true
                try {
                    val data = getGfxDataUseCase()
                    displayData = data
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isRefreshing = false
                }
                delay(2000) // 每 2 秒刷新一次
            }
        }
    }

    /**
     * 停止监测
     */
    fun stopMonitoring() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * 清理资源
     */
    fun onCleared() {
        stopMonitoring()
        viewModelScope.cancel()
    }
}