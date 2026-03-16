package sophon.desktop.feature.i18n.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class I18nProject(
    val absolutePath: String = "",
    val modules: List<I18nModule> = emptyList()
) {
    val isValid: Boolean
        get() = absolutePath.isNotBlank() && modules.isNotEmpty()
}

@Serializable
data class I18nModule(
    val name: String = "",
    val level: Int = 0,
    val absolutePath: String = ""
) : Comparable<I18nModule> {
    override fun compareTo(other: I18nModule): Int {
        return this.name.compareTo(other.name)
    }
}

data class I18nConfig(
    val toolPath: String,
    val csvPath: String,
    val modulePath: String,
    val overrideMode: Boolean
)
