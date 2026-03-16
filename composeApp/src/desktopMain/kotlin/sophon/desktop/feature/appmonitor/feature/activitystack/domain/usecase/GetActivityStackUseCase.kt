package sophon.desktop.feature.appmonitor.feature.activitystack.domain.usecase

import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.repository.ActivityStackRepository

/**
 * 获取 Activity 栈信息的用例
 */
class GetActivityStackUseCase(private val repository: ActivityStackRepository) {
    /**
     * 根据包名获取Activity栈信息
     * 
     * @param packageName 应用包名
     * @return Activity栈组件列表
     */
    suspend operator fun invoke(packageName: String): List<LifecycleComponent> {
        return repository.getActivityStack(packageName)
    }
}
