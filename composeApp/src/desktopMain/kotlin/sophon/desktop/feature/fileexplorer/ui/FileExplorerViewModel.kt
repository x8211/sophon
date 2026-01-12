package sophon.desktop.feature.fileexplorer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.fileexplorer.data.repository.FileExplorerRepositoryImpl
import sophon.desktop.feature.fileexplorer.domain.model.AppDirectoryInfo
import sophon.desktop.feature.fileexplorer.domain.model.FileItem
import sophon.desktop.feature.fileexplorer.domain.usecase.GetAppDirectoriesUseCase
import sophon.desktop.feature.fileexplorer.domain.usecase.GetFileListUseCase
import sophon.desktop.feature.fileexplorer.domain.usecase.ReadFileContentUseCase

/**
 * UI状态密封类
 */
sealed class FileExplorerUiState {
    /** 加载中 */
    data object Loading : FileExplorerUiState()
    
    /** 显示应用目录选择界面 */
    data class ShowDirectories(val appInfo: AppDirectoryInfo) : FileExplorerUiState()
    
    /** 显示文件列表 */
    data class ShowFiles(
        val currentPath: String,
        val files: List<FileItem>,
        val pathHistory: List<String>
    ) : FileExplorerUiState()
    
    /** 显示文件内容 */
    data class ShowFileContent(
        val fileName: String,
        val filePath: String,
        val content: String,
        val isXml: Boolean
    ) : FileExplorerUiState()
    
    /** 错误状态 */
    data class Error(val message: String) : FileExplorerUiState()
}

/**
 * 文件浏览器ViewModel
 * 
 * 管理文件浏览器的状态和业务逻辑
 */
class FileExplorerViewModel : ViewModel() {
    
    private val repository = FileExplorerRepositoryImpl()
    private val getFileListUseCase = GetFileListUseCase(repository)
    private val getAppDirectoriesUseCase = GetAppDirectoriesUseCase(repository)
    private val readFileContentUseCase = ReadFileContentUseCase(repository)
    
    private val _uiState = MutableStateFlow<FileExplorerUiState>(FileExplorerUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // 路径历史记录,用于返回上级目录
    private val pathHistory = mutableListOf<String>()
    
    // 保存当前浏览的目录状态,用于从文件查看返回
    private var lastFileListState: FileExplorerUiState.ShowFiles? = null
    
    init {
        loadAppDirectories()
    }
    
    /**
     * 获取当前前台应用包名
     */
    private suspend fun getForegroundPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }
    
    /**
     * 加载应用目录信息
     */
    fun loadAppDirectories() {
        viewModelScope.launch {
            _uiState.value = FileExplorerUiState.Loading
            try {
                val packageName = getForegroundPackageName()
                if (packageName.isBlank()) {
                    _uiState.value = FileExplorerUiState.Error("无法获取前台应用包名,请确保设备已连接且有应用在前台运行")
                    return@launch
                }
                
                val appInfo = getAppDirectoriesUseCase(packageName)
                if (appInfo != null) {
                    _uiState.value = FileExplorerUiState.ShowDirectories(appInfo)
                } else {
                    _uiState.value = FileExplorerUiState.Error("无法获取应用目录信息")
                }
            } catch (e: Exception) {
                _uiState.value = FileExplorerUiState.Error("加载失败: ${e.message}")
            }
        }
    }
    
    /**
     * 浏览指定目录
     * 
     * @param path 目录路径
     */
    fun browseDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = FileExplorerUiState.Loading
            try {
                val files = getFileListUseCase(path)
                pathHistory.add(path)
                val newState = FileExplorerUiState.ShowFiles(
                    currentPath = path,
                    files = files,
                    pathHistory = pathHistory.toList()
                )
                lastFileListState = newState
                _uiState.value = newState
            } catch (e: Exception) {
                _uiState.value = FileExplorerUiState.Error("加载目录失败: ${e.message}")
            }
        }
    }
    
    /**
     * 查看文件内容
     * 
     * @param file 文件项
     */
    fun viewFileContent(file: FileItem) {
        // 检查文件扩展名
        val extension = file.name.substringAfterLast('.', "").lowercase()
        val supportedExtensions = listOf("txt", "xml", "json", "log", "properties", "gradle", "md", "kt", "java")
        
        if (extension !in supportedExtensions) {
            _uiState.value = FileExplorerUiState.Error("不支持的文件类型: .$extension\n仅支持文本文件 (txt, xml, json, log, properties, gradle, md, kt, java)")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = FileExplorerUiState.Loading
            try {
                val content = readFileContentUseCase(file.path)
                val isXml = extension == "xml"
                _uiState.value = FileExplorerUiState.ShowFileContent(
                    fileName = file.name,
                    filePath = file.path,
                    content = content,
                    isXml = isXml
                )
            } catch (e: Exception) {
                _uiState.value = FileExplorerUiState.Error("读取文件失败: ${e.message}")
            }
        }
    }
    
    /**
     * 从文件查看返回到文件列表
     */
    fun backToFileList() {
        lastFileListState?.let {
            _uiState.value = it
        } ?: navigateBack()
    }
    
    /**
     * 返回上级目录
     */
    fun navigateBack() {
        if (pathHistory.size <= 1) {
            // 返回到目录选择界面
            pathHistory.clear()
            lastFileListState = null
            loadAppDirectories()
            return
        }
        
        // 移除当前路径
        pathHistory.removeLastOrNull()
        
        // 获取上级路径
        val parentPath = pathHistory.lastOrNull()
        if (parentPath != null) {
            // 移除上级路径,因为browseDirectory会重新添加
            pathHistory.removeLastOrNull()
            browseDirectory(parentPath)
        }
    }
    
    /**
     * 刷新当前目录
     */
    fun refresh() {
        val currentState = _uiState.value
        if (currentState is FileExplorerUiState.ShowFiles) {
            // 移除当前路径,然后重新加载
            pathHistory.removeLastOrNull()
            browseDirectory(currentState.currentPath)
        } else {
            loadAppDirectories()
        }
    }
}
