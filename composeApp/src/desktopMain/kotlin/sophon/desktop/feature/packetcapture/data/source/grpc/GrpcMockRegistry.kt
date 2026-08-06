package sophon.desktop.feature.packetcapture.data.source.grpc

import sophon.desktop.feature.packetcapture.model.GrpcMockRule

/**
 * gRPC Mock 规则注册表，供 Netty EventLoop 线程快速查询。
 *
 * 使用 `@Volatile` + 不可变列表替换（无锁）保证线程安全：
 * ViewModel 在主/IO 线程写入，Netty handler 在 EventLoop 线程只读。
 */
internal object GrpcMockRegistry {

    data class MockResult(
        val encodedBody: ByteArray,
        val grpcStatus: Int,
    )

    private data class Entry(val rule: GrpcMockRule, val encodedBody: ByteArray)

    @Volatile
    private var entries: List<Entry> = emptyList()

    /** 原子替换全部规则（已预编码，encodedBody 为 null 的规则跳过）。 */
    fun update(rules: List<GrpcMockRule>, encodedBodies: Map<String, ByteArray>) {
        entries = rules
            .filter { it.enabled }
            .mapNotNull { rule ->
                val body = encodedBodies[rule.id] ?: return@mapNotNull null
                Entry(rule, body)
            }
    }

    /**
     * 按 host + path 查找第一条匹配的规则。
     * host 为 "*" 时匹配所有 host。
     */
    fun findMatch(host: String, path: String): MockResult? {
        val current = entries
        for (entry in current) {
            val rule = entry.rule
            val hostMatch = rule.host == "*" || rule.host == host
            val pathMatch = rule.path == path
            if (hostMatch && pathMatch) {
                return MockResult(entry.encodedBody, rule.grpcStatus)
            }
        }
        return null
    }
}
