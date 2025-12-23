package sophon.desktop.feature.i18n.data.source

import kotlinx.serialization.Serializable
import sophon.desktop.core.datastore.createDataStore
import sophon.desktop.feature.i18n.domain.model.I18nProject

@Serializable
data class I18nToolConfig(val toolPath: String = "")

val i18nToolDataStore = createDataStore(
    fileName = "i18n.pb",
    defaultValue = I18nToolConfig(),
    serializer = I18nToolConfig.serializer()
)

val i18nProjectDataStore = createDataStore(
    fileName = "project.pb",
    defaultValue = I18nProject(),
    serializer = I18nProject.serializer()
)
