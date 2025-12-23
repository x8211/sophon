package sophon.desktop.feature.apps.domain.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.apps.domain.model.AppLoadState

/**
 * 已安装应用仓库接口
 */
interface InstalledAppsRepository {
    /**
     * 获取已安装的第三方应用列表
     * @return 包含加载状态和数据的 Flow
     */
    fun getInstalledApps(): Flow<AppLoadState>
}
