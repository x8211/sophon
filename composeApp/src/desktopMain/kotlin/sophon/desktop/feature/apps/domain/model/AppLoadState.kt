package sophon.desktop.feature.apps.domain.model

/**
 * 应用加载状态
 */
sealed class AppLoadState {
    /** 初始化/闲置 */
    data object Idle : AppLoadState()
    
    /** 正在加载 */
    data object Loading : AppLoadState()
    
    /** 加载进度 */
    data class Progress(val current: Int, val total: Int) : AppLoadState()
    
    /** 加载完成 */
    data class Success(val apps: List<AppInfo>) : AppLoadState()
    
    /** 加载失败 */
    data class Error(val message: String) : AppLoadState()
}
