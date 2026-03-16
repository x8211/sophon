package sophon.desktop.feature.appmonitor.feature.thread.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.appmonitor.feature.thread.data.repository.ThreadRepositoryImpl
import sophon.desktop.feature.appmonitor.feature.thread.domain.model.ProcessInfo
import sophon.desktop.feature.appmonitor.feature.thread.domain.usecase.ThreadUseCase

/**
 * 线程信息ViewModel
 * 
 * 根据传入的包名加载线程信息
 */
class ThreadViewModel(
    private val useCase: ThreadUseCase = ThreadUseCase(ThreadRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessInfo?>(null)
    val uiState = _uiState.asStateFlow()

    /**
     * 根据包名加载线程信息
     * 
     * @param packageName 应用包名
     */
    fun loadThreadInfo(packageName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = useCase.getProcessByPackageName(packageName)
            } catch (_: Exception) {
                _uiState.value = null
            }
        }
    }
}
