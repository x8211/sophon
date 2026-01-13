package sophon.desktop.feature.appmonitor.domain.usecase

import sophon.desktop.feature.appmonitor.domain.model.AppInfo
import sophon.desktop.feature.appmonitor.domain.repository.AppMonitorRepository

/**
 * 获取前台应用信息用例
 * 
 * 封装获取当前前台应用包名和debuggable状态的业务逻辑
 * 
 * @property repository 应用监控仓库
 */
class GetForegroundAppInfoUseCase(
    private val repository: AppMonitorRepository
) {
    
    /**
     * 执行用例
     * 
     * @return 应用信息，如果获取失败返回null
     */
    suspend operator fun invoke(): AppInfo? {
        return repository.getForegroundAppInfo()
    }
}
