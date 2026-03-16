package sophon.desktop.feature.adb.domain.usecase

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.adb.domain.model.AdbState
import sophon.desktop.feature.adb.domain.repository.AdbRepository

/**
 * 获取 ADB 状态流的用例
 */
class GetAdbStateUseCase(private val repository: AdbRepository) {
    operator fun invoke(): Flow<AdbState> = repository.getAdbState()
}
