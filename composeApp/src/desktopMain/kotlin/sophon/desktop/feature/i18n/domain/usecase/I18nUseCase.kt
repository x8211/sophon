package sophon.desktop.feature.i18n.domain.usecase

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.i18n.domain.model.I18nConfig
import sophon.desktop.feature.i18n.domain.model.I18nProject
import sophon.desktop.feature.i18n.domain.repository.I18nRepository

class I18nUseCase(private val repository: I18nRepository) {

    suspend fun parseProjectStructure(path: String): I18nProject {
        return repository.parseProjectStructure(path)
    }

    suspend fun findI18nToolPath(): String {
        return repository.findI18nToolPath()
    }

    fun executeTranslation(config: I18nConfig): Flow<String> {
        return repository.executeTranslation(config)
    }
}
