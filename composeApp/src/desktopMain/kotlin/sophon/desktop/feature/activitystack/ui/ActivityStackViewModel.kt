package sophon.desktop.feature.activitystack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.feature.activitystack.data.repository.ActivityStackRepositoryImpl
import sophon.desktop.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.activitystack.domain.usecase.GetActivityStackUseCase

class ActivityStackViewModel(
    private val getActivityStackUseCase: GetActivityStackUseCase = GetActivityStackUseCase(ActivityStackRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<LifecycleComponent>>(emptyList())
    val uiState = _uiState.asStateFlow()

    // 用于定时刷新
    private val _ticker = MutableSharedFlow<Long>()

    init {
        // 每2秒触发一次刷新信号
        viewModelScope.launch {
            while (true) {
                _ticker.emit(System.currentTimeMillis())
                delay(2000)
            }
        }
        
        // 当 Context (设备连接状态) 变化或 Ticker 触发时，刷新数据
        viewModelScope.launch {
            combine(Context.stream, _ticker) { context, _ -> context }
                .collect {
                    // 获取数据
                    _uiState.value = getActivityStackUseCase()
                }
        }
    }
}
