package sophon.desktop.feature.update.model

import kotlinx.serialization.Serializable

/** DataStore 持久化存储的更新偏好设置。 */
@Serializable
data class UpdatePrefs(
    /** 用户选择忽略的版本号；为空表示未忽略任何版本。 */
    val ignoredVersion: String = ""
)
