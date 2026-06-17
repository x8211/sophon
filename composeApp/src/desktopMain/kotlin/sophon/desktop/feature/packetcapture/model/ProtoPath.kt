package sophon.desktop.feature.packetcapture.model

import kotlinx.serialization.Serializable

/**
 * 用户配置的 proto 解析路径，可以是单个 `.proto` 文件或包含多个 `.proto` 的目录。
 * 使用 kotlinx-serialization 持久化到 `~/.sophon/proto_paths.json`。
 */
@Serializable
data class ProtoPath(
    val path: String,
    val isDirectory: Boolean
)
