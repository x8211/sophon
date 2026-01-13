package sophon.desktop.feature.appmonitor.feature.activitystack.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.LifecycleComponent
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.repository.ActivityStackRepository

/**
 * ActivityStackRepository 实现
 * 
 * 通过ADB命令获取指定应用的Activity栈信息
 */
class ActivityStackRepositoryImpl : ActivityStackRepository {

    override suspend fun getActivityStack(packageName: String): List<LifecycleComponent> {
        if (packageName.isBlank()) {
            return emptyList()
        }
        return queryDetail(packageName)
    }

    /**
     * 查询指定包名的Activity栈详细信息
     * 
     * @param packageName 应用包名
     * @return Activity栈组件列表
     */
    private suspend fun queryDetail(packageName: String): List<LifecycleComponent> {
        if (packageName.isBlank()) return emptyList()
        // dumpsys activity <package/component> 并解析
        return "adb shell dumpsys activity $packageName".oneshotShell {
            ActivityStackParser.parse(it)
        }
    }
}
