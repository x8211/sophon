package sophon.desktop.feature.systemmonitor.feature.gfx.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sophon.desktop.feature.systemmonitor.feature.gfx.data.repository.GfxRepositoryImpl
import sophon.desktop.feature.systemmonitor.feature.gfx.domain.model.DisplayData
import sophon.desktop.feature.systemmonitor.feature.gfx.domain.usecase.GetGfxDataUseCase

/**
 * 图形监测 ViewModel
 * 负责管理数据加载、定时刷新和状态维护
 */
class GfxViewModel : ViewModel() {
    private val repository = GfxRepositoryImpl()
    private val getGfxDataUseCase = GetGfxDataUseCase(repository)
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    // 当前显示的图形数据
    var displayData by mutableStateOf<DisplayData?>(null)
        private set

    // 是否正在刷新
    var isRefreshing by mutableStateOf(false)
        private set

    /**
     * 刷新数据
     */
    fun refresh() {
        if (isRefreshing) return

        viewModelScope.launch {
            isRefreshing = true
            try {
                val data = getGfxDataUseCase()
                displayData = data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshing = false
            }
        }
    }

}