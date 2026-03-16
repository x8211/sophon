package sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.usecase

import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.FileItem
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.repository.FileExplorerRepository

/**
 * 获取文件列表用例
 * 
 * 封装获取指定目录下文件列表的业务逻辑
 * 
 * @property repository 文件浏览器仓库
 */
class GetFileListUseCase(
    private val repository: FileExplorerRepository
) {
    /**
     * 执行用例
     * 
     * @param path 目录路径
     * @return 文件项列表,按目录优先、名称排序
     */
    suspend operator fun invoke(path: String): List<FileItem> {
        val files = repository.getFileList(path)
        // 排序:目录在前,文件在后,同类型按名称排序
        return files.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }
}
