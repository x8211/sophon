package sophon.desktop.feature.systemmonitor.feature.cpu.domain.model

/**
 * CPU负载信息
 * @param load1min 1分钟平均负载
 * @param load5min 5分钟平均负载
 * @param load15min 15分钟平均负载
 */
data class CpuLoadInfo(
    val load1min: Float = 0f,
    val load5min: Float = 0f,
    val load15min: Float = 0f
)

/**
 * CPU使用时间段信息
 * @param startTime 开始时间
 * @param endTime 结束时间
 * @param durationMs 持续时间(毫秒)
 */
data class CpuTimeRange(
    val startTime: String = "",
    val endTime: String = "",
    val durationMs: Long = 0
)

/**
 * 进程CPU使用详情
 * @param pid 进程ID
 * @param processName 进程名称
 * @param totalPercent 总CPU使用率
 * @param userPercent 用户态CPU使用率
 * @param kernelPercent 内核态CPU使用率
 * @param minorFaults 次要页错误数
 * @param majorFaults 主要页错误数
 */
data class ProcessCpuInfo(
    val pid: Int = 0,
    val processName: String = "",
    val totalPercent: Float = 0f,
    val userPercent: Float = 0f,
    val kernelPercent: Float = 0f,
    val minorFaults: Int = 0,
    val majorFaults: Int = 0
)

/**
 * 系统整体CPU使用率
 * @param totalPercent 总CPU使用率
 * @param userPercent 用户态CPU使用率
 * @param kernelPercent 内核态CPU使用率
 * @param iowaitPercent IO等待CPU使用率
 * @param irqPercent 硬中断CPU使用率
 * @param softirqPercent 软中断CPU使用率
 */
data class SystemCpuInfo(
    val totalPercent: Float = 0f,
    val userPercent: Float = 0f,
    val kernelPercent: Float = 0f,
    val iowaitPercent: Float = 0f,
    val irqPercent: Float = 0f,
    val softirqPercent: Float = 0f
)

/**
 * CPU监测数据汇总
 * @param loadInfo CPU负载信息
 * @param timeRange CPU使用时间段
 * @param processList 所有进程CPU使用列表
 * @param systemCpu 系统整体CPU使用率
 */
data class CpuData(
    val loadInfo: CpuLoadInfo = CpuLoadInfo(),
    val timeRange: CpuTimeRange = CpuTimeRange(),
    val processList: List<ProcessCpuInfo> = emptyList(),
    val systemCpu: SystemCpuInfo = SystemCpuInfo()
)
