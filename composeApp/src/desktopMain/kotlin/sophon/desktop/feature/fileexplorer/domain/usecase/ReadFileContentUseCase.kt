package sophon.desktop.feature.fileexplorer.domain.usecase

import sophon.desktop.feature.fileexplorer.domain.repository.FileExplorerRepository

/**
 * 读取文件内容用例
 * 
 * 封装读取文件内容的业务逻辑
 * 
 * @property repository 文件浏览器仓库
 */
class ReadFileContentUseCase(
    private val repository: FileExplorerRepository
) {
    /**
     * 执行用例
     * 
     * @param path 文件路径
     * @return 文件内容字符串
     */
    suspend operator fun invoke(path: String): String {
        return repository.readFileContent(path)
    }
}
