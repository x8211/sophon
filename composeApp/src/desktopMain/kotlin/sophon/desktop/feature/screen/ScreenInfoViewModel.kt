package sophon.desktop.feature.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Context

class ScreenInfoViewModel : ViewModel() {

    private val dataSource = ScreenInfoDataSource()

    private val _uiState = MutableStateFlow(ScreenMetaData())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            Context.stream.collect {
                updateScreenInfo()
            }
        }
    }

    private suspend fun updateScreenInfo() {
        val screenInfo = dataSource.queryScreenInfo()
        _uiState.update {
            it.copy(
                physicalWidth = screenInfo.physicalWidth,
                physicalHeight = screenInfo.physicalHeight,
                physicalDensity = screenInfo.physicalDensity,
                overrideWidth = screenInfo.overrideWidth,
                overrideHeight = screenInfo.overrideHeight,
                overrideDensity = screenInfo.overrideDensity
            )
        }
    }

    fun modifyWidthInput(width: String) = _uiState.update { it.copy(inputWidth = width) }
    fun modifyHeightInput(height: String) = _uiState.update { it.copy(inputHeight = height) }
    fun modifyDensityInput(density: String) = _uiState.update { it.copy(inputDensity = density) }

    fun modifyResolution() {
        viewModelScope.launch {
            dataSource.modifyResolution(_uiState.value.inputWidth, _uiState.value.inputHeight)
            updateScreenInfo()
        }
    }

    fun modifyDensity() {
        viewModelScope.launch {
            dataSource.modifyDensity(_uiState.value.inputDensity)
            updateScreenInfo()
        }
    }

    fun resetResolution() {
        viewModelScope.launch {
            dataSource.resetResolution()
            updateScreenInfo()
        }
    }

    fun resetDensity() {
        viewModelScope.launch {
            dataSource.resetDensity()
            updateScreenInfo()
        }
    }

}