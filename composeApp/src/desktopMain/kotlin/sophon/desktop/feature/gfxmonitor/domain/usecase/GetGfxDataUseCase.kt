package sophon.desktop.feature.gfxmonitor.domain.usecase

import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData
import sophon.desktop.feature.gfxmonitor.domain.repository.GfxMonitorRepository

/**
 * 获取图形监测数据的用例
 */
class GetGfxDataUseCase(private val repository: GfxMonitorRepository) {
    /**
     * 执行获取数据逻辑
     * @return 返回 DisplayData 对象
     */
    suspend operator fun invoke(): DisplayData {
        return repository.getDisplayData()
    }
}
