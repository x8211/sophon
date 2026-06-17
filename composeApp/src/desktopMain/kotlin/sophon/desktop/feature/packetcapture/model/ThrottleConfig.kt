package sophon.desktop.feature.packetcapture.model

/**
 * 预设网络限速档位，每档包含下载/上传带宽（bytes/sec）。
 * [downloadBps]/[uploadBps] 为 0 表示该方向不限速。
 */
enum class ThrottlePreset(val label: String, val downloadBps: Long, val uploadBps: Long) {
    NONE("不限速", 0L, 0L),
    SLOW_3G("慢速 3G (400 Kbps)", 51_200L, 51_200L),
    FAST_3G("快速 3G (1.5 Mbps)", 192_000L, 96_000L),
    LTE("LTE (20 Mbps)", 2_560_000L, 1_280_000L),
    WIFI("WiFi (50 Mbps)", 6_400_000L, 3_200_000L),
    CUSTOM("自定义", 0L, 0L),
}

/**
 * 限速配置，持有当前选中的 [preset] 及自定义档位下的下载/上传速率（Kbps）。
 * [effectiveDownloadBps]/[effectiveUploadBps] 为最终生效的字节速率：
 * - 非 CUSTOM 档位直接取枚举值
 * - CUSTOM 档位将 Kbps（kilobits/sec）换算为 bytes/sec：N Kbps = N × 1024 / 8 = N × 128 B/s
 * - 任一方向为 0 表示不限速
 */
data class ThrottleConfig(
    val preset: ThrottlePreset = ThrottlePreset.NONE,
    val customDownloadKbps: Long = 1_000L,
    val customUploadKbps: Long = 500L,
) {
    val isActive: Boolean get() = preset != ThrottlePreset.NONE

    val effectiveDownloadBps: Long
        get() = if (preset == ThrottlePreset.CUSTOM) customDownloadKbps * 128L else preset.downloadBps

    val effectiveUploadBps: Long
        get() = if (preset == ThrottlePreset.CUSTOM) customUploadKbps * 128L else preset.uploadBps

    val displayLabel: String
        get() = if (preset == ThrottlePreset.CUSTOM) {
            "↓${customDownloadKbps}K ↑${customUploadKbps}K"
        } else {
            preset.label
        }
}
