package sophon.desktop.feature.installapk.domain.usecase

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.installapk.domain.repository.InstallApkRepository

class InstallApkUseCase(private val repository: InstallApkRepository) {
    fun execute(path: String): Flow<String> {
        return repository.installApk(path)
    }
}
