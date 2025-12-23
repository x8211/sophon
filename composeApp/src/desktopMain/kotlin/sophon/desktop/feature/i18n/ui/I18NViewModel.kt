package sophon.desktop.feature.i18n.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.feature.i18n.data.repository.I18nRepositoryImpl
import sophon.desktop.feature.i18n.data.source.i18nProjectDataStore
import sophon.desktop.feature.i18n.data.source.i18nToolDataStore
import sophon.desktop.feature.i18n.domain.model.I18nConfig
import sophon.desktop.feature.i18n.domain.model.I18nProject
import sophon.desktop.feature.i18n.domain.usecase.I18nUseCase

/**
 * 多语言功能ViewModel
 */
class I18NViewModel(
    private val useCase: I18nUseCase = I18nUseCase(I18nRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Load initial state
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            i18nProjectDataStore.data.collect {
                if (it.absolutePath.isNotBlank()) {
                    val project = useCase.parseProjectStructure(it.absolutePath)
                    _uiState.update { state ->
                        state.copy(
                            project = project,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { state -> state.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            i18nToolDataStore.data.collect {
                if (it.toolPath.isBlank()) {
                    val foundPath = useCase.findI18nToolPath()
                    if (foundPath.isNotBlank()) {
                        _uiState.update { state -> state.copy(toolPath = foundPath) }
                    }
                } else {
                    _uiState.update { state -> state.copy(toolPath = it.toolPath) }
                }
            }
        }
    }

    fun updateToolPath(toolPath: String) =
        _uiState.update { state -> state.copy(toolPath = toolPath) }

    fun importProject(projectFilePath: String?) {
        if (projectFilePath == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = useCase.parseProjectStructure(projectFilePath)
            if (project.isValid) {
                i18nProjectDataStore.updateData { it.copy(absolutePath = projectFilePath) }
            }
            _uiState.update {
                it.copy(
                    project = project,
                    isLoading = false
                )
            }
        }
    }

    fun importCsv(csvFilePath: String?) {
        csvFilePath ?: return
        _uiState.update { it.copy(csvPath = csvFilePath) }
    }

    fun selectModule(modulePath: String) = _uiState.update { it.copy(modulePath = modulePath) }

    fun setOverrideMode(overrideMode: Boolean) =
        _uiState.update { it.copy(overrideMode = overrideMode) }

    fun translate() {
        // 先清除之前的输出和状态
        clearOutput()

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(commandOutput = "正在执行...\n") }

                // 保存工具路径到DataStore
                i18nToolDataStore.updateData { it.copy(toolPath = _uiState.value.toolPath) }

                // 保存项目路径到DataStore
                if (_uiState.value.project.isValid) {
                    i18nProjectDataStore.updateData {
                        it.copy(absolutePath = _uiState.value.project.absolutePath)
                    }
                }

                val config = _uiState.value.run {
                    I18nConfig(
                        toolPath = toolPath,
                        csvPath = csvPath,
                        modulePath = modulePath,
                        overrideMode = overrideMode
                    )
                }
                useCase.executeTranslation(config).collect { output ->
                    _uiState.update { it.copy(commandOutput = it.commandOutput + output) }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(commandOutput = it.commandOutput + "执行出错: ${e.message}\n${e.stackTraceToString()}")
                }
            }
        }
    }

    fun clearOutput() = _uiState.update { it.copy(commandOutput = "") }

    fun reset() {
        _uiState.value = UiState()
    }
}

/**
 * 统一的UI状态数据类
 */
data class UiState(
    val project: I18nProject = I18nProject(), // 项目状态
    val isLoading: Boolean = false, // 是否正在加载
    val toolPath: String = "", // 工具路径
    val csvPath: String = "", // CSV文件路径
    val modulePath: String = "", // 模块路径
    val overrideMode: Boolean = false, // 冲突时是否覆盖原值
    val commandOutput: String = "", // 命令输出内容
)
