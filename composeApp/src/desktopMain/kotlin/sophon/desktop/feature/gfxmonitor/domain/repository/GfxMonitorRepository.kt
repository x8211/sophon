package sophon.desktop.feature.gfxmonitor.domain.repository

import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData

/**
 * 图形监测数据仓库接口
 */
interface GfxMonitorRepository {
    /**
     * 获取显示性能数据 (gfxinfo)
     */
    suspend fun getDisplayData(): DisplayData
}
