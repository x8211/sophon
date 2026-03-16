package sophon.desktop.feature.installapk.data.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.core.Shell.streamShell
import sophon.desktop.feature.installapk.domain.repository.InstallApkRepository

class InstallApkRepositoryImpl : InstallApkRepository {
    override fun installApk(path: String): Flow<String> {
        return "adb install -r \"$path\"".streamShell()
    }
}
