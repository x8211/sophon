package sophon.desktop.feature.device.domain.usecase

import sophon.desktop.feature.device.domain.model.DeviceInfoSection
import sophon.desktop.feature.device.domain.repository.DeviceInfoRepository

/**
 * 获取设备信息的用例
 * 封装了获取设备详情的业务逻辑
 *
 * @property repository 设备信息仓库
 */
class GetDeviceInfoUseCase(private val repository: DeviceInfoRepository) {
    /**
     * 执行用例
     * @return 设备信息板块列表
     */
    suspend operator fun invoke(): List<DeviceInfoSection> {
        return repository.getDeviceInfo()
    }
}
