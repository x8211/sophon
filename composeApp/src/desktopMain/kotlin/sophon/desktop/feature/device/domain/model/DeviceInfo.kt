package sophon.desktop.feature.device.domain.model

/**
 * 设备信息的单个条目
 * @property name 条目名称 (e.g., "Device Model")
 * @property value 条目值 (e.g., "Pixel 6")
 */
data class DeviceInfoItem(val name: String, val value: String)

/**
 * 设备信息的一个板块
 * @property name 板块名称 (e.g., "Basic Info", "CPU Info")
 * @property items 该板块下的信息列表
 */
data class DeviceInfoSection(
    val name: String,
    val items: List<DeviceInfoItem>
)
