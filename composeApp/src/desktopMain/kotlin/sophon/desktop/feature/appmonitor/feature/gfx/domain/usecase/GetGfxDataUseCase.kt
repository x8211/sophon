package sophon.desktop.feature.appmonitor.feature.gfx.domain.usecase

import sophon.desktop.feature.appmonitor.feature.gfx.domain.model.DisplayData
import sophon.desktop.feature.appmonitor.feature.gfx.domain.repository.GfxRepository

/**
 * 获取图形监测数据的用例
 */
class GetGfxDataUseCase(private val repository: GfxRepository) {
    /**
     * 执行获取数据逻辑
     * @return 返回 DisplayData 对象
     */
    suspend operator fun invoke(packageName: String): DisplayData {
        return repository.getDisplayData(packageName)
    }
}
