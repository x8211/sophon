package sophon.desktop.feature.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.core.Context

class DeviceInfoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<List<DeviceInfoSection>>(emptyList())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            Context.stream.collect {
                _uiState.value = collectInfo()
            }
        }
    }
}