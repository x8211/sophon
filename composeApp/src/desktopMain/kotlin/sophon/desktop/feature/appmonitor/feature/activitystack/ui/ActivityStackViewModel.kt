package sophon.desktop.feature.appmonitor.feature.activitystack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.appmonitor.feature.activitystack.data.repository.ActivityStackRepositoryImpl
import sophon.desktop.feature.appmonitor.feature.activitystack.model.LifecycleComponent

/**
 * Activity栈ViewModel
 * 
 * 根据传入的包名加载Activity栈信息
 */
class ActivityStackViewModel(
    private val repository: ActivityStackRepositoryImpl = ActivityStackRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<LifecycleComponent>>(emptyList())
    val uiState = _uiState.asStateFlow()

    /**
     * 根据包名加载Activity栈信息
     * 
     * @param packageName 应用包名
     */
    fun loadActivityStack(packageName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = repository.getActivityStack(packageName)
            } catch (_: Exception) {
                _uiState.value = emptyList()
            }
        }
    }
}
