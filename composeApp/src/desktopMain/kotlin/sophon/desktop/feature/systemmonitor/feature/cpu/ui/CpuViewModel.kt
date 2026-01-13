package sophon.desktop.feature.systemmonitor.feature.cpu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.systemmonitor.feature.cpu.data.repository.CpuRepositoryImpl
import sophon.desktop.feature.systemmonitor.feature.cpu.domain.model.CpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.domain.usecase.GetCpuDataUseCase

/**
 * CPU监测ViewModel
 */
class CpuViewModel : ViewModel() {

    private val getCpuDataUseCase = GetCpuDataUseCase(CpuRepositoryImpl())

    private val _uiState = MutableStateFlow<CpuUiState>(CpuUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentPackageName: String? = null

    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                // 如果是手动触发，不设置为 Loading，以避免界面闪烁
                val data = getCpuDataUseCase()
                _uiState.value = CpuUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = CpuUiState.Error(e.message ?: "未知错误")
            }
        }
    }

}


/**
 * CPU监测UI状态
 */
sealed class CpuUiState {
    data object Loading : CpuUiState()
    data class Success(val data: CpuData) : CpuUiState()
    data class Error(val message: String) : CpuUiState()
}
