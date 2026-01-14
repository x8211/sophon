package sophon.desktop.feature.appmonitor.feature.gfx.domain.repository

import sophon.desktop.feature.appmonitor.feature.gfx.domain.model.DisplayData

/**
 * 图形监测数据仓库接口
 */
interface GfxRepository {
    /**
     * 获取显示性能数据 (gfxinfo)
     */
    suspend fun getDisplayData(packageName: String): DisplayData
}
