package sophon.desktop.feature.systemmonitor.feature.cpu.common.model

/**
 * 线程CPU使用详情
 * 公共数据模型，被dumpsys和realtime两个功能共享使用
 * @param tid 线程ID
 * @param threadName 线程名称
 * @param totalPercent 总CPU使用率
 * @param userPercent 用户态CPU使用率
 * @param kernelPercent 内核态CPU使用率
 */
data class ThreadCpuInfo(
    val tid: Int = 0,
    val threadName: String = "",
    val totalPercent: Float = 0f,
    val userPercent: Float = 0f,
    val kernelPercent: Float = 0f
)
