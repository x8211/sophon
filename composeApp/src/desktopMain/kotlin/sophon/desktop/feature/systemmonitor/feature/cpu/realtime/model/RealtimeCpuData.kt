package sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model

/**
 * 实时CPU进程信息
 * 从top命令输出解析得到
 * @param pid 进程ID
 * @param user 用户名
 * @param priority 优先级
 * @param niceValue Nice值
 * @param virtualMemory 虚拟内存
 * @param residentMemory 常驻内存
 * @param sharedMemory 共享内存
 * @param status 进程状态 (R=运行, S=睡眠, D=不可中断睡眠, Z=僵尸, T=停止)
 * @param cpuPercent CPU使用率百分比
 * @param memPercent 内存使用率百分比
 * @param time CPU时间
 * @param processName 进程名称
 */
data class RealtimeProcessInfo(
    val pid: Int = 0,
    val user: String = "",
    val priority: Int = 0,
    val niceValue: Int = 0,
    val virtualMemory: String = "",
    val residentMemory: String = "",
    val sharedMemory: String = "",
    val status: String = "",
    val cpuPercent: Float = 0f,
    val memPercent: Float = 0f,
    val time: String = "",
    val processName: String = ""
)

/**
 * 实时系统CPU信息
 * @param totalCpu 总CPU使用率 (800%表示8核全部使用)
 * @param userPercent 用户态CPU使用率
 * @param nicePercent Nice进程CPU使用率
 * @param sysPercent 系统态CPU使用率
 * @param idlePercent 空闲CPU百分比
 * @param iowaitPercent IO等待CPU使用率
 * @param irqPercent 硬中断CPU使用率
 * @param softirqPercent 软中断CPU使用率
 * @param hostPercent 主机CPU使用率
 */
data class RealtimeSystemCpuInfo(
    val totalCpu: Float = 0f,
    val userPercent: Float = 0f,
    val nicePercent: Float = 0f,
    val sysPercent: Float = 0f,
    val idlePercent: Float = 0f,
    val iowaitPercent: Float = 0f,
    val irqPercent: Float = 0f,
    val softirqPercent: Float = 0f,
    val hostPercent: Float = 0f
) {
    /**
     * 获取实际使用的CPU百分比（总CPU - 空闲）
     */
    fun getUsedPercent(): Float = totalCpu - idlePercent
}

/**
 * 实时任务统计信息
 * @param total 总任务数
 * @param running 运行中任务数
 * @param sleeping 睡眠中任务数
 * @param stopped 停止的任务数
 * @param zombie 僵尸任务数
 */
data class RealtimeTaskStats(
    val total: Int = 0,
    val running: Int = 0,
    val sleeping: Int = 0,
    val stopped: Int = 0,
    val zombie: Int = 0
)

/**
 * 实时内存信息
 * @param total 总内存
 * @param used 已使用内存
 * @param free 空闲内存
 * @param buffers 缓冲区内存
 */
data class RealtimeMemoryInfo(
    val total: String = "",
    val used: String = "",
    val free: String = "",
    val buffers: String = ""
)

/**
 * 实时Swap信息
 * @param total 总Swap
 * @param used 已使用Swap
 * @param free 空闲Swap
 * @param cached 缓存Swap
 */
data class RealtimeSwapInfo(
    val total: String = "",
    val used: String = "",
    val free: String = "",
    val cached: String = ""
)

/**
 * 实时CPU监测数据汇总
 * @param taskStats 任务统计信息
 * @param memoryInfo 内存信息
 * @param swapInfo Swap信息
 * @param systemCpu 系统CPU信息
 * @param processList 进程列表（按CPU使用率降序排列）
 */
data class RealtimeCpuData(
    val taskStats: RealtimeTaskStats = RealtimeTaskStats(),
    val memoryInfo: RealtimeMemoryInfo = RealtimeMemoryInfo(),
    val swapInfo: RealtimeSwapInfo = RealtimeSwapInfo(),
    val systemCpu: RealtimeSystemCpuInfo = RealtimeSystemCpuInfo(),
    val processList: List<RealtimeProcessInfo> = emptyList()
)
