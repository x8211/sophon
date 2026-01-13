package sophon.desktop.feature.systemmonitor.feature.gfx.domain.repository

import sophon.desktop.feature.systemmonitor.feature.gfx.domain.model.DisplayData

/**
 * 图形监测数据仓库接口
 */
interface GfxRepository {
    /**
     * 获取显示性能数据 (gfxinfo)
     */
    suspend fun getDisplayData(): DisplayData
}
