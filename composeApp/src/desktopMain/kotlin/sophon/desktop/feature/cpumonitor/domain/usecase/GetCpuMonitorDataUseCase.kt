package sophon.desktop.feature.cpumonitor.domain.usecase

import sophon.desktop.feature.cpumonitor.domain.model.CpuMonitorData
import sophon.desktop.feature.cpumonitor.domain.repository.CpuMonitorRepository

/**
 * 获取CPU监测数据用例
 */
class GetCpuMonitorDataUseCase(
    private val repository: CpuMonitorRepository
) {
    /**
     * 执行获取CPU监测数据
     * @param packageName 包名,如果为null则获取所有进程的CPU信息
     * @return CPU监测数据
     */
    suspend operator fun invoke(packageName: String?): CpuMonitorData {
        return repository.getCpuMonitorData(packageName)
    }
}
