package sophon.desktop.feature.systemmonitor.domain.usecase

import sophon.desktop.feature.systemmonitor.domain.repository.SystemMonitorRepository

/**
 * 获取系统信息用例
 *
 * 封装获取系统信息的业务逻辑
 *
 * @property repository 系统监控仓库
 */
class GetSystemInfoUseCase(
    private val repository: SystemMonitorRepository
) {
    /**
     * 执行用例
     */
    suspend operator fun invoke(): Long {
        return repository.getTimestamp()
    }
}
