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
        override val error: String? = null
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
        override val error: String? = null
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
