package sophon.desktop.feature.installapk.data.repository

import kotlinx.coroutines.flow.Flow

interface InstallApkRepository {
    fun installApk(path: String): Flow<String>
}
