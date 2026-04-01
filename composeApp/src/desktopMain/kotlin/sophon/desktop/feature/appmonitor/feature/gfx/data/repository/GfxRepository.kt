package sophon.desktop.feature.appmonitor.feature.gfx.data.repository

import sophon.desktop.feature.appmonitor.feature.gfx.model.DisplayData

/**
 * 图形监测数据仓库接口
 */
interface GfxRepository {
    /**
     * 获取显示性能数据 (gfxinfo)
     */
    suspend fun getDisplayData(packageName: String): DisplayData
}
