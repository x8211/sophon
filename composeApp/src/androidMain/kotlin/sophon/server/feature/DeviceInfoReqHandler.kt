package sophon.server.feature

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import sophon.common.protobuf.response.DEVICE_INFO_RSP
import sophon.common.protobuf.response.DeviceInfoItem
import sophon.common.protobuf.response.DeviceInfoRsp
import sophon.common.protobuf.response.DeviceInfoSection
import sophon.common.protobuf.response.ResponseContext
import sophon.common.util.FormatTool.formatMemorySize
import sophon.server.RequestHandler

class DeviceInfoReqHandler : RequestHandler {
    override fun handle(request: String): ResponseContext {
        // 获取设备基本信息
        val deviceInfoSections = getDeviceInfo()
        return ResponseContext.create(DEVICE_INFO_RSP, DeviceInfoRsp(deviceInfoSections))
    }

    @SuppressLint("NewApi")
    private fun getDeviceInfo(): List<DeviceInfoSection> {
        val sections = mutableListOf<DeviceInfoSection>()

        // 设备基本信息
        val basicInfoItems = mutableListOf<DeviceInfoItem>().apply {
            add(DeviceInfoItem("设备品牌", Build.BRAND))
            add(DeviceInfoItem("设备制造商", Build.MANUFACTURER))
            add(DeviceInfoItem("设备型号", Build.MODEL))
            add(DeviceInfoItem("设备代号", Build.DEVICE))
            add(DeviceInfoItem("产品名称", Build.PRODUCT))
            add(DeviceInfoItem("主板", Build.BOARD))
            add(DeviceInfoItem("硬件名称", Build.HARDWARE))
            add(DeviceInfoItem("设备指纹", Build.FINGERPRINT))
        }
        sections.add(DeviceInfoSection("设备基本信息", basicInfoItems))

        // 系统信息
        val systemInfoItems = mutableListOf<DeviceInfoItem>().apply {
            add(DeviceInfoItem("Android 版本", Build.VERSION.RELEASE))
            add(DeviceInfoItem("API 级别", Build.VERSION.SDK_INT.toString()))
            add(DeviceInfoItem("构建ID", Build.ID))
            add(DeviceInfoItem("构建时间", Build.TIME.toString()))
            add(DeviceInfoItem("安全补丁级别", Build.VERSION.SECURITY_PATCH))
        }
        sections.add(DeviceInfoSection("系统信息", systemInfoItems))

        // CPU/处理器信息
        val cpuInfoItems = mutableListOf<DeviceInfoItem>().apply {
            add(DeviceInfoItem("CPU ABI", Build.SUPPORTED_ABIS.joinToString(", ")))
            add(DeviceInfoItem("SoC制造商", Build.SOC_MANUFACTURER))
            add(DeviceInfoItem("SoC型号", Build.SOC_MODEL))
        }
        sections.add(DeviceInfoSection("CPU/处理器信息", cpuInfoItems))

        // 存储信息
        val storageInfoItems = mutableListOf<DeviceInfoItem>()
        try {
            val statFs = android.os.StatFs(Environment.getExternalStorageDirectory().path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalSize = totalBlocks * blockSize / 1024
            val availableSize = availableBlocks * blockSize / 1024

            storageInfoItems.add(DeviceInfoItem("总存储空间", formatMemorySize(totalSize)))
            storageInfoItems.add(DeviceInfoItem("可用存储空间", formatMemorySize(availableSize)))
        } catch (e: Exception) {
            storageInfoItems.add(DeviceInfoItem("获取存储信息失败", e.message ?: "未知错误"))
        }
        sections.add(DeviceInfoSection("存储信息", storageInfoItems))

        return sections
    }
}