package sophon.desktop.feature.appmonitor.domain.model

/**
 * 应用信息数据模型
 * 
 * @property packageName 应用包名
 * @property isDebuggable 是否为debuggable模式
 */
data class AppInfo(
    val packageName: String,
    val isDebuggable: Boolean
)
