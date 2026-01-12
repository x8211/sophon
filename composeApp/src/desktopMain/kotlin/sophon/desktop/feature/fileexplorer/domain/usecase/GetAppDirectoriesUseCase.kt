package sophon.desktop.feature.fileexplorer.domain.usecase

import sophon.desktop.feature.fileexplorer.domain.model.AppDirectoryInfo
import sophon.desktop.feature.fileexplorer.domain.repository.FileExplorerRepository

/**
 * 获取应用目录用例
 * 
 * 封装获取应用数据目录和缓存目录的业务逻辑
 * 
 * @property repository 文件浏览器仓库
 */
class GetAppDirectoriesUseCase(
    private val repository: FileExplorerRepository
) {
    /**
     * 执行用例
     * 
     * @param packageName 应用包名
     * @return 应用目录信息,如果获取失败返回null
     */
    suspend operator fun invoke(packageName: String): AppDirectoryInfo? {
        return repository.getAppDirectories(packageName)
    }
}
