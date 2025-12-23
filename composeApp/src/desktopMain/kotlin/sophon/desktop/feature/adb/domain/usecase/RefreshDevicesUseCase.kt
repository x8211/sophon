package sophon.desktop.feature.adb.domain.usecase

import sophon.desktop.feature.adb.domain.repository.AdbRepository

/**
 * 刷新连接设备列表的用例
 */
class RefreshDevicesUseCase(private val repository: AdbRepository) {
    suspend operator fun invoke() = repository.refreshDevices()
}
