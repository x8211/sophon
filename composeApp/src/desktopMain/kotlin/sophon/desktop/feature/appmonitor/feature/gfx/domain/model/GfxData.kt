package sophon.desktop.feature.appmonitor.feature.gfx.domain.model

/**
 * 通用的图形性能指标
 */
data class GfxMetrics(
    val totalFrames: Int = 0,
    val jankyFrames: Int = 0,
    val jankPercentage: Float = 0f,
    // CPU Percentiles
    val p50Cpu: Float = 0f,
    val p90Cpu: Float = 0f,
    val p95Cpu: Float = 0f,
    val p99Cpu: Float = 0f,
    // GPU Percentiles
    val p50Gpu: Float = 0f,
    val p90Gpu: Float = 0f,
    val p95Gpu: Float = 0f,
    val p99Gpu: Float = 0f,
    // Reasons
    val jankReasons: List<JankReason> = emptyList()
)

/**
 * 显示性能监测数据 (Derived from gfxinfo)
 */
data class DisplayData(
    val packageName: String = "",
    val globalMetrics: GfxMetrics = GfxMetrics(),
    // View Hierarchy
    val totalViewRootImpl: Int = 0,
    val totalViews: Int = 0,
    val renderNodeUsedMemory: String = "",
    val renderNodeCapacityMemory: String = "",
    val viewRootDetails: List<ViewRootInfo> = emptyList()
)

/**
 * 单个 ViewRoot 的详细信息
 */
data class ViewRootInfo(
    val name: String,
    val views: Int,
    val renderNodeMemory: String,
    val metrics: GfxMetrics = GfxMetrics()
)

/**
 * 掉帧原因
 */
data class JankReason(
    val rawKey: String,
    val count: Int,
    val description: String
)
