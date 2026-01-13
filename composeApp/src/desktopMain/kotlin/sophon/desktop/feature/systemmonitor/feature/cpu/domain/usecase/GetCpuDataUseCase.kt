package sophon.desktop.feature.systemmonitor.feature.cpu.domain.usecase

import sophon.desktop.feature.systemmonitor.feature.cpu.domain.model.CpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.domain.repository.CpuRepository

/**
 * 获取CPU监测数据用例
 */
class GetCpuDataUseCase(
    private val repository: CpuRepository
) {
    /**
     * 执行获取CPU监测数据
     * @return CPU监测数据
     */
    suspend operator fun invoke(): CpuData {
        return repository.getCpuData()
    }
}
