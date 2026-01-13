package sophon.desktop.feature.activitystack

import kotlinx.coroutines.runBlocking
import org.junit.Test
import sophon.desktop.feature.appmonitor.feature.activitystack.data.repository.ActivityStackRepositoryImpl
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.usecase.GetActivityStackUseCase

class GetActivityStackTest {

    @Test
    fun start() {
        runBlocking {
            val useCase = GetActivityStackUseCase(ActivityStackRepositoryImpl())
            val result = useCase()
            result.forEach { printChild(it, 0) }
        }
    }

    private fun printChild(lifecycleComponent: LifecycleComponent, level: Int) {
        println("${"    ".repeat(level)}${lifecycleComponent.name()}")
        lifecycleComponent.children().forEach {
            printChild(it, level + 1)
        }
    }

}