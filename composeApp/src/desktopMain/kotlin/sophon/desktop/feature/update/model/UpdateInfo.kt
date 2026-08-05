package sophon.desktop.feature.update.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 服务端返回的版本信息，对应 version.json 文件结构。
 *
 * 示例：
 * ```json
 * {
 *   "version": "1.2.0",
 *   "releaseNotes": "修复若干 Bug，提升稳定性",
 *   "downloadUrl": {
 *     "macos": "https://example.com/Sophon-1.2.0.dmg",
 *     "windows": "https://example.com/Sophon-1.2.0.msi"
 *   }
 * }
 * ```
 */
@Serializable
data class UpdateInfo(
    val version: String,
    val releaseNotes: String = "",
    val downloadUrl: DownloadUrls = DownloadUrls()
)

@Serializable
data class DownloadUrls(
    val macos: String = "",
    val windows: String = ""
)
