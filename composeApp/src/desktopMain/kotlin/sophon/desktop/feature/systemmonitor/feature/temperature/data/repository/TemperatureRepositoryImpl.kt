package sophon.desktop.feature.systemmonitor.feature.temperature.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.systemmonitor.feature.temperature.domain.model.MonitorDataPoint
import sophon.desktop.feature.systemmonitor.feature.temperature.domain.model.TemperatureData
import sophon.desktop.feature.systemmonitor.feature.temperature.domain.repository.TemperatureRepository

/**
 * 系统监测数据仓库实现
 * 通过 ADB Shell 命令获取设备的温度和帧率数据
 */
class TemperatureRepositoryImpl : TemperatureRepository {

    // 保存历史数据点(最多保留60个数据点,约2分钟的数据)
    private val temperatureHistory = mutableListOf<MonitorDataPoint>()
    private val maxHistorySize = 60

    override suspend fun getTemperatureData(): TemperatureData {
        return try {
            // 通过 ADB 命令获取电池温度
            val output = "adb shell dumpsys battery".oneshotShell { it }
            
            // 解析温度数据(单位是0.1摄氏度)
            val tempLine = output.lines().find { it.trim().startsWith("temperature:") }
            val tempRaw = tempLine?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
            val currentTemp = tempRaw / 10f // 转换为摄氏度
            
            // 添加到历史记录
            val dataPoint = MonitorDataPoint(
                timestamp = System.currentTimeMillis(),
                value = currentTemp
            )
            temperatureHistory.add(dataPoint)
            
            if (temperatureHistory.size > maxHistorySize) {
                temperatureHistory.removeAt(0)
            }
            
            val temps = temperatureHistory.map { it.value }
            TemperatureData(
                dataPoints = temperatureHistory.toList(),
                currentTemp = currentTemp,
                maxTemp = temps.maxOrNull() ?: currentTemp,
                minTemp = temps.minOrNull() ?: currentTemp,
                avgTemp = temps.average().toFloat()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            TemperatureData()
        }
    }
}
