package sophon.desktop.feature.adb.domain.usecase

import sophon.desktop.feature.adb.domain.repository.AdbRepository

/**
 * 更新 ADB 路径的用例
 */
class UpdateAdbPathUseCase(private val repository: AdbRepository) {
    suspend operator fun invoke(path: String) = repository.updateAdbPath(path)
}
