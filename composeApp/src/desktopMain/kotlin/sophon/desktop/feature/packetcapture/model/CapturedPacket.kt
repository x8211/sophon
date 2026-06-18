package sophon.desktop.feature.packetcapture.model

/**
 * 单条抓包记录的不可变数据模型，以 [id] 为唯一标识。
 *
 * 通过密封接口明确区分两种协议类型：
 * - [Http]：普通 HTTP/HTTPS 请求
 * - [Grpc]：gRPC 请求（Content-Type: application/grpc，运行在 HTTP/2 之上）
 *
 * 公共属性与派生计算属性定义在接口层，子类只需实现字段声明；
 * [equals]/[hashCode] 均仅基于 [id]，避免 [ByteArray] 引用比较引发的问题。
 */
sealed interface CapturedPacket {

    val id: Long
    val timestamp: Long
    val method: String
    val scheme: String
    val host: String
    val path: String
    val requestHeaders: Map<String, String>
    val requestBody: ByteArray?
    val statusCode: Int?
    val responseHeaders: Map<String, String>
    val responseBody: ByteArray?
    val durationMs: Long?
    val error: String?
    /**
     * 文件下载响应的完整响应体临时文件（仅当响应被识别为文件下载时存在）。
     * 保存完成或包记录清除后应删除此文件。
     */
    val responseBodyFile: java.io.File?

    val url: String get() = "$scheme://$host$path"
    val isComplete: Boolean get() = statusCode != null || error != null
    val statusText: String get() = statusCode?.toString() ?: error?.let { "ERR" } ?: "..."

    fun requestBodyAsText(): String? {
        val body = requestBody ?: return null
        val encoding = (requestHeaders["content-encoding"] ?: requestHeaders["Content-Encoding"])
            ?.lowercase()?.trim()
        return runCatching {
            when (encoding) {
                "gzip" -> java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(body))
                    .readBytes()
                    .toString(Charsets.UTF_8)
                "deflate" -> {
                    runCatching {
                        java.util.zip.InflaterInputStream(java.io.ByteArrayInputStream(body))
                            .readBytes()
                            .toString(Charsets.UTF_8)
                    }.getOrElse {
                        java.util.zip.InflaterInputStream(
                            java.io.ByteArrayInputStream(body),
                            java.util.zip.Inflater(true)
                        ).readBytes().toString(Charsets.UTF_8)
                    }
                }
                else -> body.toString(Charsets.UTF_8)
            }
        }.getOrElse { body.toString(Charsets.UTF_8) }
    }

    fun responseBodyAsText(): String? {
        val body = responseBody ?: return null
        val encoding = (responseHeaders["content-encoding"] ?: responseHeaders["Content-Encoding"])
            ?.lowercase()?.trim()
        return runCatching {
            when (encoding) {
                "gzip" -> java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(body))
                    .readBytes()
                    .toString(Charsets.UTF_8)
                "deflate" -> {
                    // 部分服务器对 deflate 使用 zlib 包装，部分使用原始 deflate；优先尝试 zlib
                    runCatching {
                        java.util.zip.InflaterInputStream(java.io.ByteArrayInputStream(body))
                            .readBytes()
                            .toString(Charsets.UTF_8)
                    }.getOrElse {
                        java.util.zip.InflaterInputStream(
                            java.io.ByteArrayInputStream(body),
                            java.util.zip.Inflater(true)
                        ).readBytes().toString(Charsets.UTF_8)
                    }
                }
                else -> body.toString(Charsets.UTF_8)
            }
        }.getOrElse { body.toString(Charsets.UTF_8) }
    }

    fun requestBodySize(): Int = requestBody?.size ?: 0
    fun responseBodySize(): Int = responseBody?.size ?: 0

    /** 普通 HTTP/HTTPS 请求记录。 */
    data class Http(
        override val id: Long,
        override val timestamp: Long,
        override val method: String,
        override val scheme: String,
        override val host: String,
        override val path: String,
        override val requestHeaders: Map<String, String> = emptyMap(),
        override val requestBody: ByteArray? = null,
        override val statusCode: Int? = null,
        override val responseHeaders: Map<String, String> = emptyMap(),
        override val responseBody: ByteArray? = null,
        override val durationMs: Long? = null,
        override val error: String? = null,
        override val responseBodyFile: java.io.File? = null,
    ) : CapturedPacket {
        override fun equals(other: Any?): Boolean = other is CapturedPacket && id == other.id
        override fun hashCode(): Int = id.hashCode()
    }

    /**
     * gRPC 请求记录（基于 HTTP/2，Content-Type: application/grpc）。
     *
     * [path] 格式为 `/{ServiceName}/{MethodName}`，
     * 由此派生出 [service] 和 [rpcMethod] 两个计算属性供 UI 展示。
     */
    data class Grpc(
        override val id: Long,
        override val timestamp: Long,
        override val method: String,
        override val scheme: String,
        override val host: String,
        override val path: String,
        override val requestHeaders: Map<String, String> = emptyMap(),
        override val requestBody: ByteArray? = null,
        override val statusCode: Int? = null,
        override val responseHeaders: Map<String, String> = emptyMap(),
        override val responseBody: ByteArray? = null,
        override val durationMs: Long? = null,
        override val error: String? = null,
        override val responseBodyFile: java.io.File? = null,
    ) : CapturedPacket {
        /** 从路径解析的 gRPC 服务名，格式 `/{ServiceName}/{MethodName}` → `ServiceName`。 */
        val service: String? get() {
            val trimmed = path.trimStart('/')
            val idx = trimmed.indexOf('/')
            return if (idx > 0) trimmed.substring(0, idx) else null
        }

        /** 从路径解析的 RPC 方法名，格式 `/{ServiceName}/{MethodName}` → `MethodName`。 */
        val rpcMethod: String? get() {
            val trimmed = path.trimStart('/')
            val idx = trimmed.indexOf('/')
            return if (idx > 0 && idx < trimmed.lastIndex) trimmed.substring(idx + 1) else null
        }

        override fun equals(other: Any?): Boolean = other is CapturedPacket && id == other.id
        override fun hashCode(): Int = id.hashCode()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 文件下载检测扩展
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 判断此条记录的响应是否为文件下载（二进制/压缩/媒体类型）。
 * 满足以下任一条件即返回 true：
 * 1. Content-Disposition 含 "attachment"
 * 2. Content-Type 为二进制/压缩/媒体 MIME 类型，且排除 text 类型、application/json、application/grpc、application/xml
 */
fun CapturedPacket.isFileDownload(): Boolean {
    val headers = responseHeaders.entries.associateBy({ it.key.lowercase() }, { it.value })
    val disposition = headers["content-disposition"]
    if (disposition?.contains("attachment", ignoreCase = true) == true) return true

    val contentType = headers["content-type"]?.lowercase()?.substringBefore(";")?.trim()
        ?: return false

    val textTypes = listOf("text/", "application/json", "application/grpc", "application/xml")
    if (textTypes.any { contentType.startsWith(it) }) return false

    val binaryPrefixes = listOf(
        "application/zip", "application/gzip", "application/x-tar",
        "application/pdf", "application/octet-stream",
        "application/vnd.", "application/x-",
        "image/", "audio/", "video/",
    )
    return binaryPrefixes.any { contentType.startsWith(it) }
}

/** 从响应头提取文件下载元信息（文件名、大小、MD5 等）。 */
fun CapturedPacket.fileDownloadInfo(): FileDownloadInfo {
    val headers = responseHeaders.entries.associateBy({ it.key.lowercase() }, { it.value })
    val disposition = headers["content-disposition"] ?: ""
    val fileName = Regex("""filename[*]?=["']?([^"';\r\n]+)""")
        .find(disposition)?.groupValues?.get(1)?.trim()
        ?: path.substringAfterLast('/').substringBefore('?').ifBlank { "未知文件" }
    return FileDownloadInfo(
        fileName = fileName,
        contentType = headers["content-type"] ?: "",
        sizeBytes = headers["content-length"]?.toLongOrNull() ?: -1L,
        md5 = headers["content-md5"],
        etag = headers["etag"],
        lastModified = headers["last-modified"],
    )
}
