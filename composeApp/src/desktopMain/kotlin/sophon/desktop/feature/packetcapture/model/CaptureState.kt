package sophon.desktop.feature.packetcapture.model

/** 抓包服务的运行状态：未启动、运行中或发生错误。 */
enum class CaptureStatus { STOPPED, RUNNING, ERROR }

/**
 * 抓包功能的 UI 状态聚合，作为界面层的单一数据源。
 * [filteredPackets] 和 [selectedPacket] 为基于 [packets]、[filterText]、[selectedPacketId] 实时计算的派生属性。
 */
data class CaptureState(
    val status: CaptureStatus = CaptureStatus.STOPPED,
    val port: Int = 8888,
    val packets: List<CapturedPacket> = emptyList(),
    val selectedPacketId: Long? = null,
    val filterText: String = "",
    val errorMessage: String? = null,
    val showCaInstallGuide: Boolean = false,
    /** ADB 推送 CA 证书是否成功（true = 已推送到 Android 设备）。 */
    val caAndroidPushed: Boolean = false,
    /** 本机 CA 证书文件的绝对路径，用于在对话框中展示给用户。 */
    val caCertPath: String = "",
    val expandedHosts: Set<String> = emptySet(),
    /** 用户配置的 .proto 路径列表（文件或目录）。 */
    val protoPaths: List<ProtoPath> = emptyList(),
    /** 是否显示 Proto Schema 管理对话框。 */
    val showProtoManager: Boolean = false,
    /** 已成功加载的 Schema 消息描述符数量；-1 表示尚未加载。 */
    val schemaLoadedCount: Int = -1,
    /** Schema 加载出错时的错误信息。 */
    val schemaLoadError: String? = null,
    /** 当前限速配置；[ThrottleConfig.isActive] 为 false 时表示不限速。 */
    val throttleConfig: ThrottleConfig = ThrottleConfig(),
    /** 是否显示限速配置对话框。 */
    val showThrottleDialog: Boolean = false,
    /** 已在后台解码完成的包体内容，key 为 [CapturedPacket.id]。 */
    val decodedBodies: Map<Long, DecodedBody> = emptyMap(),
    /** true 表示当前选中包正在后台解码，界面应显示加载状态。 */
    val isDecodingBody: Boolean = false,
    /**
     * 包 id → CapturedPacket 的快速索引，与 [packets] 同步维护。
     * 用于 [selectedPacket] O(1) 查找，避免每次 O(n) find。
     */
    val packetIndex: Map<Long, CapturedPacket> = emptyMap(),
    /** 用户配置的 gRPC Mock 规则列表。 */
    val mockRules: List<GrpcMockRule> = emptyList(),
    /** 是否显示 gRPC Mock 管理对话框。 */
    val showMockDialog: Boolean = false,
    /** 正在对话框中编辑的规则；null 表示新建。 */
    val editingMockRule: GrpcMockRule? = null,
    /** ruleId → 编码错误信息（Schema 未找到 / JSON 解析失败）。 */
    val mockEncodeErrors: Map<String, String> = emptyMap(),
) {
    val isRunning: Boolean get() = status == CaptureStatus.RUNNING

    val filteredPackets: List<CapturedPacket>
        get() = if (filterText.isBlank()) packets
        else packets.filter {
            it.host.contains(filterText, ignoreCase = true) ||
                    it.path.contains(filterText, ignoreCase = true) ||
                    it.method.contains(filterText, ignoreCase = true) ||
                    it.statusCode?.toString()?.contains(filterText) == true
        }

    /** 按 host 分组（基于 filteredPackets，保留各 host 内的到达时序）。 */
    val groupedPackets: Map<String, List<CapturedPacket>>
        get() = filteredPackets.groupBy { it.host }

    /** O(1) 查找，依赖 [packetIndex]。 */
    val selectedPacket: CapturedPacket?
        get() = selectedPacketId?.let { packetIndex[it] }
}
