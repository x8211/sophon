package sophon.desktop.feature.appmonitor.feature.thread.domain.model

/**
 * 线程信息数据类
 * 只包含线程级别的字段
 */
data class ThreadInfo(
    val tid: String = "",        // 线程ID
    val wchan: String = "",      // 等待通道
    val address: String = "",    // 内存地址
    val state: String = "",      // 线程状态: R=运行中, S=睡眠, D=不可中断睡眠, Z=僵尸, T=被跟踪或已停止
    val cmd: String = "",        // 线程命令/名称
)
