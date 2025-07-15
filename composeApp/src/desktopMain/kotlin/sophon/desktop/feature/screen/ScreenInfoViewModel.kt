package sophon.desktop.feature.screen

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Context
import sophon.desktop.core.SophonSocketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScreenInfoViewModel : ScreenModel {

    private val dataSource = ScreenInfoDataSource()

    private val _uiState = MutableStateFlow(ScreenMetaData())
    val uiState = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            Context.stream.collect {
                updateScreenInfo()
            }
        }
        SophonSocketRepository.sendData("queryPackageInfo")
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
        screenModelScope.launch {
            dataSource.modifyResolution(_uiState.value.inputWidth, _uiState.value.inputHeight)
            updateScreenInfo()
        }
    }

    fun modifyDensity() {
        screenModelScope.launch {
            dataSource.modifyDensity(_uiState.value.inputDensity)
            updateScreenInfo()
        }
    }

    fun resetResolution() {
        screenModelScope.launch {
            dataSource.resetResolution()
            updateScreenInfo()
        }
    }

    fun resetDensity() {
        screenModelScope.launch {
            dataSource.resetDensity()
            updateScreenInfo()
        }
    }

}