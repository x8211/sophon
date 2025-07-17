package sophon.desktop.feature.i18n

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.datastore.i18nDataStore
import sophon.desktop.datastore.projectDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sophon.desktop.pb.Project
import sophon.desktop.pb.Module
import java.io.File

/**
 * 多语言功能ViewModel
 */
class I18NViewModel : StateScreenModel<UiState>(UiState()) {

    init {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            projectDataStore.data.collect {
                mutableState.update { state ->
                    state.copy(
                        project = it.absolutePath.toProjectStructure(),
                        isLoading = false
                    )
                }
            }
        }

        screenModelScope.launch {
            i18nDataStore.data.collect {
                if (it.toolPath.isBlank()) {
                    try {
                        // 获取系统环境变量
                        val env = System.getenv()
                        val path = env["PATH"] ?: ""

                        // 使用whereis命令获取路径，并确保加载完整的shell环境
                        val shellCommand = """
                            export PATH='/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:$path'
                            whereis i18n
                        """.trimIndent()

                        val i18nToolPath =
                            shellCommand.simpleShell().substringAfter("i18n:", "").trim()

                        mutableState.update { state -> state.copy(toolPath = i18nToolPath) }

                    } catch (e: Exception) {
                        println("执行命令时出错: ${e.message}\n${e.stackTraceToString()}")
                    }
                } else {
                    mutableState.update { state -> state.copy(toolPath = it.toolPath) }
                }
            }
        }
    }

    fun updateToolPath(toolPath: String) = mutableState.update { it.copy(toolPath = toolPath) }

    fun importProject(projectFilePath: String?) {
        if (projectFilePath == null) return
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            val structure = projectFilePath.toProjectStructure()
            if (structure.isValid) projectDataStore.updateData {
                it.copy(absolutePath = projectFilePath)
            }
            mutableState.value = UiState(
                project = structure,
                toolPath = mutableState.value.toolPath
            )
        }
    }

    fun importCsv(csvFilePath: String?) {
        csvFilePath ?: return
        mutableState.update { it.copy(csvPath = csvFilePath) }
    }

    fun selectModule(modulePath: String) =
        mutableState.update { it.copy(modulePath = modulePath) }

    fun setOverrideMode(overrideMode: Boolean) =
        mutableState.update { it.copy(overrideMode = overrideMode) }

    fun updateCurrentStep(step: Int) {
        mutableState.update { it.copy(currentStep = step) }
    }

    fun updateSelectedTabIndex(index: Int) {
        mutableState.update { it.copy(selectedTabIndex = index) }
    }

    fun translate() {
        // 先清除之前的输出和状态
        clearOutput()

        // 执行后自动切换到输出选项卡
        mutableState.update { it.copy(selectedTabIndex = 1) }

        screenModelScope.launch {
            try {
                mutableState.update {
                    it.copy(
                        isExecuting = true,
                        executionCompleted = false,
                        commandOutput = "正在执行...\n"
                    )
                }

                // 保存工具路径到DataStore
                i18nDataStore.updateData {
                    it.copy(toolPath = mutableState.value.toolPath)
                }

                // 保存项目路径到DataStore
                projectDataStore.updateData {
                    it.copy(absolutePath = mutableState.value.project.absolutePath)
                }

                // 执行i18n命令
                val state = mutableState.value
                val command = if (state.overrideMode) {
                    "${state.toolPath} append --src ${state.csvPath} --out ${state.modulePath}/src/main/res --nolint --prefer-new"
                } else {
                    "${state.toolPath} append --src ${state.csvPath} --out ${state.modulePath}/src/main/res --nolint"
                }

                mutableState.update { it.copy(commandOutput = it.commandOutput + "执行命令: $command\n\n") }

                try {
                    val result = command.simpleShell()
                    mutableState.update {
                        it.copy(
                            commandOutput = it.commandOutput + result,
                            executionCompleted = true,
                            progress = 1.0f
                        )
                    }
                } catch (e: Exception) {
                    mutableState.update {
                        it.copy(
                            commandOutput = it.commandOutput + "命令执行失败: ${e.message}\n"
                        )
                    }
                    throw e
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        commandOutput = it.commandOutput + "执行出错: ${e.message}\n${e.stackTraceToString()}"
                    )
                }
            } finally {
                mutableState.update { it.copy(isExecuting = false) }
            }
        }
    }

    fun clearOutput() {
        mutableState.update { it.copy(commandOutput = "", executionCompleted = false) }
    }

    fun reset() {
        mutableState.update {
            it.copy(
                project = Project(),
                isLoading = false,
                commandOutput = "",
                executionCompleted = false,
                progress = 0f
            )
        }
    }
}

private suspend fun String.toProjectStructure(): Project {
    return withContext(Dispatchers.IO) {
        val structs = mutableListOf<Module>()
        buildModulesWithRes(this@toProjectStructure, 0, structs)
        Project(this@toProjectStructure, structs)
    }
}

/**
 * 寻找所有包含res文件夹的Module
 */
private fun buildModulesWithRes(
    parentPath: String,
    level: Int,
    result: MutableList<Module>,
) {
    val parent = File(parentPath)
    val children = parent.listFiles()?.sorted() ?: return
    if (children.isEmpty()) return
    val hit = parent.containsBuildGradle() && parent.containsResDir()
    if (hit) result.add(
        Module(parent.name, level, parent.absolutePath)
    )
    children.forEach { buildModulesWithRes(it.absolutePath, if (hit) level + 1 else level, result) }
}

/**
 * 包含build.gradle文件
 */
private fun File.containsBuildGradle(): Boolean {
    val children = listFiles() ?: return false
    return children.find { it.isFile && it.name == "build.gradle" } != null
}

/**
 * 包含res文件夹
 */
private fun File.containsResDir(): Boolean {
    val resFile = File(this.absolutePath, "src/main/res")
    return resFile.exists() && resFile.isDirectory
}

val Project.isValid
    get() = absolutePath.isNotBlank() && modules.isNotEmpty()

/**
 * 统一的UI状态数据类
 */
data class UiState(
    val project: Project = Project(), // 项目状态
    val isLoading: Boolean = false, // 是否正在加载
    val toolPath: String = "", // 工具路径
    val modulePath: String = "", // 模块路径
    val csvPath: String = "", // CSV文件路径
    val overrideMode: Boolean = false, // 冲突时是否覆盖原值
    val commandOutput: String = "", // 命令输出内容
    val isExecuting: Boolean = false, // 是否正在执行
    val executionCompleted: Boolean = false, // 是否已执行完成
    val currentStep: Int = 0, // 当前步骤
    val selectedTabIndex: Int = 0, // 选中的选项卡
    val progress: Float = 0f, // 进度条值
)