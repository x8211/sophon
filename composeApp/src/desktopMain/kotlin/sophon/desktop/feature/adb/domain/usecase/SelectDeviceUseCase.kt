package sophon.desktop.feature.adb.domain.usecase

import sophon.desktop.feature.adb.domain.repository.AdbRepository

/**
 * 选择操作设备的用例
 */
class SelectDeviceUseCase(private val repository: AdbRepository) {
    suspend operator fun invoke(deviceName: String) = repository.selectDevice(deviceName)
}
