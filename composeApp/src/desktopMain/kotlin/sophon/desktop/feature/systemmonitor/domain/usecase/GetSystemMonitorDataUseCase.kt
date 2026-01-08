package sophon.desktop.feature.systemmonitor.domain.usecase

import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData
import sophon.desktop.feature.gfxmonitor.domain.repository.GfxMonitorRepository
import sophon.desktop.feature.systemmonitor.domain.model.SystemMonitorData
import sophon.desktop.feature.systemmonitor.domain.model.TemperatureData
import sophon.desktop.feature.systemmonitor.domain.repository.SystemMonitorRepository

/**
 * 获取系统监测数据用例
 */
class GetSystemMonitorDataUseCase(
    private val repository: SystemMonitorRepository,
    private val gfxMonitorRepository: GfxMonitorRepository
) {
    /**
     * 执行获取系统监测数据
     */
    suspend operator fun invoke(monitors: Set<String>): SystemMonitorData {
        val temperatureData =
            if ("Temperature" in monitors) repository.getTemperatureData() else TemperatureData()
        val displayData = if ("Display" in monitors) gfxMonitorRepository.getDisplayData() else DisplayData()
        return SystemMonitorData(
            temperatureData = temperatureData,
            displayData = displayData
        )
    }
}