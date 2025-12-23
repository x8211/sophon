package sophon.desktop.feature.activitystack.domain.repository

import sophon.desktop.feature.activitystack.domain.model.LifecycleComponent

/**
 * Activity 栈信息仓库接口
 */
interface ActivityStackRepository {
    /**
     * 获取当前顶层 Activity 栈信息
     * @return 包含 Activity 和 Fragment 的组件列表
     */
    suspend fun getActivityStack(): List<LifecycleComponent>
}
