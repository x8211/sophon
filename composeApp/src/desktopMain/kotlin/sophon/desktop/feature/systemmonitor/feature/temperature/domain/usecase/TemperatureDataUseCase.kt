package sophon.desktop.feature.systemmonitor.feature.temperature.domain.usecase

import sophon.desktop.feature.systemmonitor.feature.temperature.domain.model.SystemMonitorData
import sophon.desktop.feature.systemmonitor.feature.temperature.domain.repository.TemperatureRepository

/**
 * 获取系统监测数据用例
 */
class TemperatureDataUseCase(private val repository: TemperatureRepository) {
    /**
     * 执行获取系统监测数据
     */
    suspend operator fun invoke(): SystemMonitorData {
        val temperatureData = repository.getTemperatureData()
        return SystemMonitorData(
            temperatureData = temperatureData,
        )
    }
}