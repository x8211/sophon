package sophon.desktop.feature.appmonitor.feature.activitystack.domain.repository

import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.LifecycleComponent

/**
 * Activity 栈信息仓库接口
 */
interface ActivityStackRepository {
    /**
     * 获取指定应用的 Activity 栈信息
     * 
     * @param packageName 应用包名
     * @return 包含 Activity 和 Fragment 的组件列表
     */
    suspend fun getActivityStack(packageName: String): List<LifecycleComponent>
}
