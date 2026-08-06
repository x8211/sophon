package sophon.desktop.feature.packetcapture.model

import kotlinx.serialization.json.JsonElement

/** 文件下载响应的元信息，从响应头中提取。 */
data class FileDownloadInfo(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val md5: String?,
    val etag: String?,
    val lastModified: String?,
) {
    fun formattedSize(): String = when {
        sizeBytes < 0 -> "未知"
        sizeBytes < 1_024 -> "$sizeBytes B"
        sizeBytes < 1_048_576 -> "%.1f KB".format(sizeBytes / 1_024.0)
        sizeBytes < 1_073_741_824 -> "%.1f MB".format(sizeBytes / 1_048_576.0)
        else -> "%.1f GB".format(sizeBytes / 1_073_741_824.0)
    }
}

/** 后台解码后的 gRPC 请求/响应体。 */
data class GrpcDecoded(
    val body: String,
    val isSchemaApplied: Boolean,
    /** 当 [isSchemaApplied] 为 true 时，对 [body] 做 pretty-print 后的 JSON 字符串（后台线程预计算）。 */
    val formattedBody: String? = null,
    /** 当 [isSchemaApplied] 为 true 时，预解析的 [JsonElement]，供 JSON 折叠树视图使用。 */
    val parsedElement: JsonElement? = null,
)

/**
 * 单条 [CapturedPacket] 在后台线程完成全部解码后的结果。
 *
 * - 文件下载：[isFileDownload] = true，[fileInfo] 填充，其他字段保持默认空值。
 * - 普通 HTTP：[requestText]/[responseText] + 可选 JSON 字段。
 * - gRPC：[grpcRequest]/[grpcResponse] + 可选 [requestText]/[responseText]（原始字节文本）。
 *
 * JSON 解析和 pretty-print 均在后台线程预计算，Composable 层直接使用结果，无需在主线程做任何重计算。
 */
data class DecodedBody(
    val isFileDownload: Boolean = false,
    val fileInfo: FileDownloadInfo? = null,
    /**
     * 是否有可保存的响应体数据（[CapturedPacket.responseBody] 或 [CapturedPacket.responseBodyFile] 非空）。
     * 用于控制 UI 中"保存响应体到文件"按钮的可用状态。
     */
    val bodyAvailable: Boolean = false,
    val requestText: String? = null,
    val responseText: String? = null,
    /** 响应体解析后的 [JsonElement]，供 JSON 折叠树视图使用；body 超过 200 KB 时为 null。 */
    val responseJson: JsonElement? = null,
    /** 请求体解析后的 [JsonElement]，供请求体 JSON 树视图使用。 */
    val requestJson: JsonElement? = null,
    /** 响应体的 pretty-print 文本，后台预计算；[responseJson] 为 null 时降级显示 [responseText]。 */
    val responsePrettyJson: String? = null,
    /** 请求体的 pretty-print 文本，后台预计算。 */
    val requestPrettyJson: String? = null,
    val grpcRequest: GrpcDecoded? = null,
    val grpcResponse: GrpcDecoded? = null,
)
