package sophon.desktop.feature.activitystack.domain.usecase

import sophon.desktop.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.activitystack.domain.repository.ActivityStackRepository

/**
 * 获取 Activity 栈信息的用例
 */
class GetActivityStackUseCase(private val repository: ActivityStackRepository) {
    suspend operator fun invoke(): List<LifecycleComponent> {
        return repository.getActivityStack()
    }
}
