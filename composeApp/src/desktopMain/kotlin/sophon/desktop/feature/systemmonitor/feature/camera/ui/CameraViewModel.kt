package sophon.desktop.feature.systemmonitor.feature.camera.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.systemmonitor.feature.camera.data.repository.CameraRepositoryImpl
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraData
import sophon.desktop.feature.systemmonitor.feature.camera.domain.usecase.GetCameraDataUseCase

/**
 * 相机监控 ViewModel
 *
 * 管理相机监控页面的 UI 状态，负责数据加载和刷新逻辑
 */
class CameraViewModel : ViewModel() {

    private val getCameraDataUseCase = GetCameraDataUseCase(CameraRepositoryImpl())

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Loading)
    val uiState = _uiState.asStateFlow()

    /**
     * 刷新相机监控数据
     *
     * 从设备获取最新的相机服务状态信息
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                // 为避免界面闪烁，不重置为 Loading 状态
                val data = getCameraDataUseCase()
                _uiState.value = CameraUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "未知错误")
            }
        }
    }
}

/**
 * 相机监控 UI 状态
 */
sealed class CameraUiState {
    /**
     * 加载中状态
     */
    data object Loading : CameraUiState()

    /**
     * 成功状态
     * @param data 相机监控数据
     */
    data class Success(val data: CameraData) : CameraUiState()

    /**
     * 错误状态
     * @param message 错误信息
     */
    data class Error(val message: String) : CameraUiState()
}
