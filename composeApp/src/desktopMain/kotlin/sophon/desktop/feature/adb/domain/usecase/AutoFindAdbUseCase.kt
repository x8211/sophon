package sophon.desktop.feature.adb.domain.usecase

import sophon.desktop.feature.adb.domain.repository.AdbRepository

/**
 * 自动查找有效 ADB 工具的用例
 */
class AutoFindAdbUseCase(private val repository: AdbRepository) {
    suspend operator fun invoke(): String? = repository.autoFindAdbTool()
}
