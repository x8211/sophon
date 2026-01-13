package sophon.desktop.feature.systemmonitor.feature.temperature.domain.repository

import sophon.desktop.feature.systemmonitor.feature.temperature.domain.model.TemperatureData

/**
 * 系统监测数据仓库接口
 */
interface TemperatureRepository {
    /**
     * 获取温度数据
     */
    suspend fun getTemperatureData(): TemperatureData
}
