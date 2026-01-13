package sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.repository

import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.AppDirectoryInfo
import sophon.desktop.feature.appmonitor.feature.fileexplorer.domain.model.FileItem

/**
 * 文件浏览器仓库接口
 * 定义文件系统操作的抽象方法
 */
interface FileExplorerRepository {
    
    /**
     * 获取指定目录下的文件列表
     * 
     * @param path 目录路径
     * @return 文件项列表
     */
    suspend fun getFileList(path: String): List<FileItem>
    
    /**
     * 获取应用的目录信息
     * 
     * @param packageName 应用包名
     * @return 应用目录信息,如果获取失败返回null
     */
    suspend fun getAppDirectories(packageName: String): AppDirectoryInfo?
    
    /**
     * 读取文件内容
     * 
     * @param path 文件路径
     * @return 文件内容字符串
     */
    suspend fun readFileContent(path: String): String
}
