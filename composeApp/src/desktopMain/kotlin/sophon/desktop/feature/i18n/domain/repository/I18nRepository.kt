package sophon.desktop.feature.i18n.domain.repository

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.i18n.domain.model.I18nConfig
import sophon.desktop.feature.i18n.domain.model.I18nProject

interface I18nRepository {
    suspend fun parseProjectStructure(path: String): I18nProject
    
    suspend fun findI18nToolPath(): String
    
    fun executeTranslation(config: I18nConfig): Flow<String>

    // Since DataStore is technically "Data Source", we can expose streams here or keep it in VM/UseCase. 
    // Usually Config persistence can be done via Repository too.
}
