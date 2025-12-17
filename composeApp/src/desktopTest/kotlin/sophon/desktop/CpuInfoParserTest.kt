package sophon.desktop

import sophon.desktop.feature.device.DeviceInfoSection

/**
 * 设备Cpu信息解析器测试类
 */
class CpuInfoParserTest : AbstractParserTest<List<DeviceInfoSection.CpuInfo.CoreInfo>>() {

    override fun fileName(): String = "cpu_info.txt"

    override fun parse(content: String): List<DeviceInfoSection.CpuInfo.CoreInfo> {
        return DeviceInfoSection.CpuInfo.parseCores(content)
    }

}