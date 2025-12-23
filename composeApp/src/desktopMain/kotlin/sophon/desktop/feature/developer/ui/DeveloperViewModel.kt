package sophon.desktop.feature.developer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.feature.developer.data.repository.DeveloperRepositoryImpl
import sophon.desktop.feature.developer.domain.model.DeveloperOptions
import sophon.desktop.feature.developer.domain.usecase.DeveloperSettingsUseCase

class DeveloperViewModel(
    private val useCase: DeveloperSettingsUseCase = DeveloperSettingsUseCase(DeveloperRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperOptions())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = useCase.getOptions()
        }
    }

    fun toggleDebugLayout() {
        viewModelScope.launch {
            useCase.toggleDebugLayout(_uiState.value.debugLayout)
            _uiState.update { it.copy(debugLayout = useCase.getOptions().debugLayout) }
        }
    }

    fun toggleHwUi() {
        viewModelScope.launch {
            useCase.toggleHwUi(_uiState.value.hwUi)
            _uiState.update { it.copy(hwUi = useCase.getOptions().hwUi) }
        }
    }

    fun toggleShowTouches() {
        viewModelScope.launch {
            useCase.toggleShowTouches(_uiState.value.showTouches)
            _uiState.update { it.copy(showTouches = useCase.getOptions().showTouches) }
        }
    }

    fun togglePointerLocation() {
        viewModelScope.launch {
            useCase.togglePointerLocation(_uiState.value.pointerLocation)
            _uiState.update { it.copy(pointerLocation = useCase.getOptions().pointerLocation) }
        }
    }

    fun toggleStrictMode() {
        viewModelScope.launch {
            useCase.toggleStrictMode(_uiState.value.strictMode)
            _uiState.update { it.copy(strictMode = useCase.getOptions().strictMode) }
        }
    }


    fun toggleForceRtl() {
        viewModelScope.launch {
            useCase.toggleForceRtl(_uiState.value.forceRtl)
            _uiState.update { it.copy(forceRtl = useCase.getOptions().forceRtl) }
        }
    }

    fun toggleStayAwake() {
        viewModelScope.launch {
            useCase.toggleStayAwake(_uiState.value.stayAwake)
            _uiState.update { it.copy(stayAwake = useCase.getOptions().stayAwake) }
        }
    }

    fun toggleShowAllANRs() {
        viewModelScope.launch {
            useCase.toggleShowAllANRs(_uiState.value.showAllANRs)
            _uiState.update { it.copy(showAllANRs = useCase.getOptions().showAllANRs) }
        }
    }

    fun toggleDontKeepActivities() {
        viewModelScope.launch {
            useCase.toggleDontKeepActivities(_uiState.value.dontKeepActivities)
            _uiState.update { it.copy(dontKeepActivities = useCase.getOptions().dontKeepActivities) }
        }
    }

    fun toggleWindowAnimationScale() {
        val newScale = nextScale(_uiState.value.windowAnimationScale)
        viewModelScope.launch {
            useCase.setWindowAnimationScale(newScale)
            _uiState.update { it.copy(windowAnimationScale = useCase.getOptions().windowAnimationScale) }
        }
    }

    fun setWindowAnimationScale(scale: Float) {
        viewModelScope.launch {
            useCase.setWindowAnimationScale(scale)
            _uiState.update { it.copy(windowAnimationScale = useCase.getOptions().windowAnimationScale) }
        }
    }

    fun toggleTransitionAnimationScale() {
        val newScale = nextScale(_uiState.value.transitionAnimationScale)
        viewModelScope.launch {
            useCase.setTransitionAnimationScale(newScale)
            _uiState.update { it.copy(transitionAnimationScale = useCase.getOptions().transitionAnimationScale) }
        }
    }

    fun setTransitionAnimationScale(scale: Float) {
        viewModelScope.launch {
            useCase.setTransitionAnimationScale(scale)
            _uiState.update { it.copy(transitionAnimationScale = useCase.getOptions().transitionAnimationScale) }
        }
    }

    fun toggleAnimatorDurationScale() {
        val newScale = nextScale(_uiState.value.animatorDurationScale)
        viewModelScope.launch {
            useCase.setAnimatorDurationScale(newScale)
            _uiState.update { it.copy(animatorDurationScale = useCase.getOptions().animatorDurationScale) }
        }
    }

    fun setAnimatorDurationScale(scale: Float) {
        viewModelScope.launch {
            useCase.setAnimatorDurationScale(scale)
            _uiState.update { it.copy(animatorDurationScale = useCase.getOptions().animatorDurationScale) }
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
