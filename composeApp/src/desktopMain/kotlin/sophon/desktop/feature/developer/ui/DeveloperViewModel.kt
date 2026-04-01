package sophon.desktop.feature.developer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.feature.developer.data.repository.DeveloperRepositoryImpl
import sophon.desktop.feature.developer.model.DeveloperOptions

class DeveloperViewModel(
    private val repository: DeveloperRepositoryImpl = DeveloperRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperOptions())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = repository.getOptions()
        }
    }

    fun toggleDebugLayout() {
        viewModelScope.launch {
            repository.setDebugLayout(!_uiState.value.debugLayout)
            _uiState.update { it.copy(debugLayout = repository.getOptions().debugLayout) }
        }
    }

    fun toggleHwUi() {
        viewModelScope.launch {
            repository.setHwUi(!_uiState.value.hwUi)
            _uiState.update { it.copy(hwUi = repository.getOptions().hwUi) }
        }
    }

    fun toggleShowTouches() {
        viewModelScope.launch {
            repository.setShowTouches(!_uiState.value.showTouches)
            _uiState.update { it.copy(showTouches = repository.getOptions().showTouches) }
        }
    }

    fun togglePointerLocation() {
        viewModelScope.launch {
            repository.setPointerLocation(!_uiState.value.pointerLocation)
            _uiState.update { it.copy(pointerLocation = repository.getOptions().pointerLocation) }
        }
    }

    fun toggleStrictMode() {
        viewModelScope.launch {
            repository.setStrictMode(!_uiState.value.strictMode)
            _uiState.update { it.copy(strictMode = repository.getOptions().strictMode) }
        }
    }

    fun toggleForceRtl() {
        viewModelScope.launch {
            repository.setForceRtl(!_uiState.value.forceRtl)
            _uiState.update { it.copy(forceRtl = repository.getOptions().forceRtl) }
        }
    }

    fun toggleStayAwake() {
        viewModelScope.launch {
            repository.setStayAwake(!_uiState.value.stayAwake)
            _uiState.update { it.copy(stayAwake = repository.getOptions().stayAwake) }
        }
    }

    fun toggleShowAllANRs() {
        viewModelScope.launch {
            repository.setShowAllANRs(!_uiState.value.showAllANRs)
            _uiState.update { it.copy(showAllANRs = repository.getOptions().showAllANRs) }
        }
    }

    fun toggleDontKeepActivities() {
        viewModelScope.launch {
            repository.setDontKeepActivities(!_uiState.value.dontKeepActivities)
            _uiState.update { it.copy(dontKeepActivities = repository.getOptions().dontKeepActivities) }
        }
    }

    fun toggleWindowAnimationScale() {
        val newScale = nextScale(_uiState.value.windowAnimationScale)
        viewModelScope.launch {
            repository.setWindowAnimationScale(newScale)
            _uiState.update { it.copy(windowAnimationScale = repository.getOptions().windowAnimationScale) }
        }
    }

    fun setWindowAnimationScale(scale: Float) {
        viewModelScope.launch {
            repository.setWindowAnimationScale(scale)
            _uiState.update { it.copy(windowAnimationScale = repository.getOptions().windowAnimationScale) }
        }
    }

    fun toggleTransitionAnimationScale() {
        val newScale = nextScale(_uiState.value.transitionAnimationScale)
        viewModelScope.launch {
            repository.setTransitionAnimationScale(newScale)
            _uiState.update { it.copy(transitionAnimationScale = repository.getOptions().transitionAnimationScale) }
        }
    }

    fun setTransitionAnimationScale(scale: Float) {
        viewModelScope.launch {
            repository.setTransitionAnimationScale(scale)
            _uiState.update { it.copy(transitionAnimationScale = repository.getOptions().transitionAnimationScale) }
        }
    }

    fun toggleAnimatorDurationScale() {
        val newScale = nextScale(_uiState.value.animatorDurationScale)
        viewModelScope.launch {
            repository.setAnimatorDurationScale(newScale)
            _uiState.update { it.copy(animatorDurationScale = repository.getOptions().animatorDurationScale) }
        }
    }

    fun setAnimatorDurationScale(scale: Float) {
        viewModelScope.launch {
            repository.setAnimatorDurationScale(scale)
            _uiState.update { it.copy(animatorDurationScale = repository.getOptions().animatorDurationScale) }
        }
    }
    
    private fun nextScale(current: Float): Float {
        return when (current) {
            0.0f -> 0.5f
            0.5f -> 1.0f
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 0.0f
        }
    }
}
