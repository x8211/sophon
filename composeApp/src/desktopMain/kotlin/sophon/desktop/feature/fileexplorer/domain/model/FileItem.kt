package sophon.desktop.feature.fileexplorer.domain.model

/**
 * 文件项数据模型
 * 
 * @property name 文件/目录名称
 * @property path 完整路径
 * @property isDirectory 是否为目录
 * @property size 文件大小(字节),目录为null
 * @property permissions 权限字符串(如 "drwxrwxr-x")
 * @property owner 所有者
 * @property group 所属组
 * @property modifiedTime 修改时间
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long?,
    val permissions: String,
    val owner: String,
    val group: String,
    val modifiedTime: String
)

/**
 * 应用目录信息
 * 
 * @property packageName 应用包名
 * @property dataDir 数据目录路径
 * @property cacheDir 缓存目录路径
 * @property externalCacheDir 外部缓存目录路径
 * @property isDebuggable 是否为debuggable模式(支持run-as命令)
 */
data class AppDirectoryInfo(
    val packageName: String,
    val dataDir: String,
    val cacheDir: String,
    val externalCacheDir: String?,
    val isDebuggable: Boolean = false
)

