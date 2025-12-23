package sophon.desktop.feature.device.domain.repository

import sophon.desktop.feature.device.domain.model.DeviceInfoSection

/**
 * 设备信息仓库接口
 * 负责定义获取设备信息的操作
 */
interface DeviceInfoRepository {
    /**
     * 获取所有设备信息板块
     * @return 包含所有设备信息板块的列表
     */
    suspend fun getDeviceInfo(): List<DeviceInfoSection>
}
