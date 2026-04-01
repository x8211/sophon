package sophon.desktop.feature.device.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.feature.device.data.repository.DeviceInfoRepositoryImpl
import sophon.desktop.feature.device.model.DeviceInfoSection

/**
 * 设备信息界面的 ViewModel
 * 负责管理界面状态和与领域层交互
 */
class DeviceInfoViewModel(
    private val repository: DeviceInfoRepositoryImpl = DeviceInfoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<DeviceInfoSection>>(emptyList())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 监听设备连接状态的变化，重新获取设备信息
            Context.stream.collect {
                _uiState.value = repository.getDeviceInfo()
            }
        }
    }
}
