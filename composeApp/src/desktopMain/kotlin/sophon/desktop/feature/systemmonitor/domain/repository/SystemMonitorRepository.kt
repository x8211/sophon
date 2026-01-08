package sophon.desktop.feature.systemmonitor.domain.repository

import sophon.desktop.feature.systemmonitor.domain.model.TemperatureData

/**
 * 系统监测数据仓库接口
 */
interface SystemMonitorRepository {
    /**
     * 获取温度数据
     */
    suspend fun getTemperatureData(): TemperatureData
}
