package sophon.desktop.feature.systemmonitor.domain.model

import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData

/**
 * 系统监测数据点
 * @param timestamp 时间戳(毫秒)
 * @param value 数值
 */
data class MonitorDataPoint(
    val timestamp: Long,
    val value: Float
)

/**
 * 温度监测数据
 * @param dataPoints 温度数据点列表
 * @param currentTemp 当前温度(摄氏度)
 * @param maxTemp 最大温度
 * @param minTemp 最小温度
 * @param avgTemp 平均温度
 */
data class TemperatureData(
    val dataPoints: List<MonitorDataPoint> = emptyList(),
    val currentTemp: Float = 0f,
    val maxTemp: Float = 0f,
    val minTemp: Float = 0f,
    val avgTemp: Float = 0f
)

/**
 * 系统监测数据汇总
 */
data class SystemMonitorData(
    val temperatureData: TemperatureData = TemperatureData(),
    val displayData: DisplayData = DisplayData()
)
