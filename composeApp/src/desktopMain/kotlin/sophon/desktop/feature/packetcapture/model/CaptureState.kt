package sophon.desktop.feature.packetcapture.model

enum class CaptureStatus { STOPPED, RUNNING, ERROR }

data class CaptureState(
    val status: CaptureStatus = CaptureStatus.STOPPED,
    val port: Int = 8888,
    val deviceProxy: String = "",
    val packets: List<CapturedPacket> = emptyList(),
    val selectedPacketId: Long? = null,
    val filterText: String = "",
    val errorMessage: String? = null,
    val showCaInstallGuide: Boolean = false
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

    val selectedPacket: CapturedPacket?
        get() = selectedPacketId?.let { id -> packets.find { it.id == id } }
}
