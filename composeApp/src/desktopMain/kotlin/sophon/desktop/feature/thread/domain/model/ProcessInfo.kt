package sophon.desktop.feature.thread.domain.model

/**
 * 进程信息数据类
 * 包含进程维度的字段和该进程下的所有线程列表
 */
data class ProcessInfo(
    val user: String = "",           // 用户名
    val pid: String = "",             // 进程ID
    val ppid: String = "",            // 父进程ID
    val vsz: String = "",             // 虚拟内存大小(KB)
    val rss: String = "",             // 常驻内存大小(KB)
    val packageName: String = "",     // 包名
    val threads: List<ThreadInfo> = emptyList()  // 该进程下的所有线程
) {
    /**
     * 获取运行中的线程数量
     */
    fun getRunningThreadCount(): Int = threads.count { it.state == "R" }
    
    /**
     * 获取睡眠中的线程数量
     */
    fun getSleepingThreadCount(): Int = threads.count { it.state == "S" }
    
    /**
     * 获取总线程数
     */
    fun getTotalThreadCount(): Int = threads.size
}
