package sophon.desktop.feature.packetcapture.model

/**
 * 单条抓包记录的不可变数据模型，包含请求与响应的完整信息。
 * 以 [id] 为唯一标识；[isComplete] 表示响应已接收或发生错误；
 * [url]、[statusText] 等为基于字段派生的计算属性。
 */
data class CapturedPacket(
    val id: Long,
    val timestamp: Long,
    val method: String,
    val scheme: String,
    val host: String,
    val path: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: ByteArray? = null,
    val statusCode: Int? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: ByteArray? = null,
    val durationMs: Long? = null,
    val error: String? = null
) {
    val url: String get() = "$scheme://$host$path"
    val isComplete: Boolean get() = statusCode != null || error != null
    val statusText: String get() = statusCode?.toString() ?: error?.let { "ERR" } ?: "..."

    fun requestBodyAsText(): String? = requestBody?.let { body ->
        runCatching { body.toString(Charsets.UTF_8) }.getOrNull()
    }

    fun responseBodyAsText(): String? = responseBody?.let { body ->
        runCatching { body.toString(Charsets.UTF_8) }.getOrNull()
    }

    fun requestBodySize(): Int = requestBody?.size ?: 0
    fun responseBodySize(): Int = responseBody?.size ?: 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CapturedPacket) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
