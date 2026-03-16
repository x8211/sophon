package sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.usecase

import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeCpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.repository.RealtimeCpuRepository

/**
 * 获取实时CPU数据用例
 * @param repository 实时CPU数据仓库
 */
class GetRealtimeCpuDataUseCase(
    private val repository: RealtimeCpuRepository
) {
    /**
     * 执行获取实时CPU数据
     * @return 实时CPU数据
     */
    suspend operator fun invoke(): RealtimeCpuData {
        return repository.getRealtimeCpuData()
    }
}
