package sophon.desktop.feature.installaab.data.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.installaab.data.source.AabKeystoreCache
import sophon.desktop.feature.installaab.model.AabInstallConfig

interface InstallAabRepository {
    fun installAab(config: AabInstallConfig): Flow<String>
    fun getKeystoreCache(): Flow<AabKeystoreCache>
    suspend fun saveKeystoreCache(cache: AabKeystoreCache)
}
