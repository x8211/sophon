package sophon.desktop.feature.activitystack.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.activitystack.domain.repository.ActivityStackRepository

/**
 * ActivityStackRepository 实现
 */
class ActivityStackRepositoryImpl : ActivityStackRepository {

    override suspend fun getActivityStack(): List<LifecycleComponent> {
        val packageName = queryPackageName()
        if (packageName.isBlank()) {
            return emptyList()
        }
        return queryDetail(packageName)
    }

    private suspend fun queryPackageName(): String {
        // 获取当前通过 grep '* Task{' 找到的栈顶 Activity 所在的包名/组件名
        // 输出示例: * Task{... #1 ... u0 com.example.app/.MainActivity ...}
        // 我们需要提取 com.example.app
        // 正则 A=\\d+:(\\S+) -> 匹配 A=userId:ComponentInfo
        // 实际上之前的 ViewModel 正则: "A=\\d+:(\\S+)"
        // "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { output ->
                "A=\\d+:(\\S+)".toRegex().find(output)?.groupValues?.getOrNull(1) ?: ""
            }
    }

    private suspend fun queryDetail(packageName: String): List<LifecycleComponent> {
        if (packageName.isBlank()) return emptyList()
        // dumpsys activity <package/component> 并解析
        return "adb shell dumpsys activity $packageName".oneshotShell {
            ActivityStackParser.parse(it)
        }
    }
}
