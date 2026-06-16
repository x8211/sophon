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
    val deviceProxy: String = "",
    val packets: List<CapturedPacket> = emptyList(),
    val selectedPacketId: Long? = null,
    val filterText: String = "",
    val errorMessage: String? = null,
    val showCaInstallGuide: Boolean = false,
    val expandedHosts: Set<String> = emptySet()
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

    val selectedPacket: CapturedPacket?
        get() = selectedPacketId?.let { id -> packets.find { it.id == id } }
}
