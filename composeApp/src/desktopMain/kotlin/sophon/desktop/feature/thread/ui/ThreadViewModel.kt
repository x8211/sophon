package sophon.desktop.feature.thread.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.feature.thread.data.repository.ThreadRepositoryImpl
import sophon.desktop.feature.thread.domain.model.ThreadInfo
import sophon.desktop.feature.thread.domain.usecase.ThreadUseCase

class ThreadViewModel(
    private val useCase: ThreadUseCase = ThreadUseCase(ThreadRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<ThreadInfo>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _ticker = MutableSharedFlow<Long>()

    init {
        viewModelScope.launch {
            while (true) {
                _ticker.emit(System.currentTimeMillis())
                delay(2000)
            }
        }
        viewModelScope.launch {
            combine(Context.stream, _ticker) { _, _ -> Unit }
                .collect {
                    _uiState.value = useCase.getThreadsForForegroundApp()
                }
        }
    }
}
