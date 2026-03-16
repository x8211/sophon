package sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.usecase

import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.CpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.repository.CpuRepository

/**
 * 获取CPU数据用例
 * @param repository CPU数据仓库
 */
class GetCpuDataUseCase(
    private val repository: CpuRepository
) {
    /**
     * 执行获取CPU数据
     * @return CPU数据
     */
    suspend operator fun invoke(): CpuData {
        return repository.getCpuData()
    }
}
