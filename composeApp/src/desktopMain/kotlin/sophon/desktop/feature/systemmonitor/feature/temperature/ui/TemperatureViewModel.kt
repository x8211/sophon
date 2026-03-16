package sophon.desktop.feature.systemmonitor.feature.temperature.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.systemmonitor.feature.temperature.data.repository.TemperatureRepositoryImpl
import sophon.desktop.feature.systemmonitor.feature.temperature.domain.model.SystemMonitorData
import sophon.desktop.feature.systemmonitor.feature.temperature.domain.usecase.TemperatureDataUseCase

/**
 * 温度监测 ViewModel
 * 
 * 负责获取温度监测数据并更新UI状态
 * 不再自动轮询，由父组件通过 refreshTrigger 触发刷新
 */
class TemperatureViewModel(
    private val getTemperatureDataUseCase: TemperatureDataUseCase =
        TemperatureDataUseCase(TemperatureRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<SystemMonitorData>(SystemMonitorData())
    val uiState = _uiState.asStateFlow()

    /**
     * 刷新温度数据
     */
    fun refresh() {
        viewModelScope.launch {
            val data = getTemperatureDataUseCase()
            _uiState.value = data
        }
    }
}
