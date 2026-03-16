package sophon.desktop.feature.installapk.domain.repository

import kotlinx.coroutines.flow.Flow

interface InstallApkRepository {
    fun installApk(path: String): Flow<String>
}
