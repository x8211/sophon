package sophon.desktop.feature.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot
import sophon.desktop.core.FormatTool
import kotlin.collections.iterator

@Slot("线程信息")
class ThreadScreen : Screen {

    // 表头提示信息
    private val columnTooltips = mapOf(
        "USER" to "用户名：拥有该线程的用户账户名称，通常表示线程运行的权限级别",
        "PID" to "进程ID：进程的唯一标识符，代表应用程序的主进程",
        "TID" to "线程ID：线程的唯一标识符，每个线程都有一个独特的TID",
        "PPID" to "父进程ID：创建当前进程的父进程ID",
        "VSZ" to "虚拟内存大小：进程分配的虚拟内存总量，包括未实际使用的内存",
        "RSS" to "常驻内存大小：进程实际占用的物理内存大小",
        "WCHAN" to "等待通道：如果线程处于睡眠状态，显示它在等待的内核函数",
        "ADDR" to "内核中的内存地址：程序计数器的当前值，或内存中的地址",
        "S" to "线程状态：R=运行中，S=睡眠，D=不可中断睡眠，Z=僵尸，T=被跟踪或已停止",
        "CMD" to "命令：线程正在执行的命令名称或函数名"
    )

    // 常见线程提示信息
    private val threadTooltips = mapOf(
        "main" to "主线程：应用程序的UI线程，负责处理用户交互和界面绘制",
        "Binder" to "Binder线程：Android的IPC通信机制，用于进程间通信",
        "AsyncTask" to "异步任务线程：用于后台操作，避免阻塞主线程",
        "OkHttp" to "OkHttp网络库线程：处理HTTP网络请求，属于Square公司开发的网络库",
        "RxCachedThreadScheduler" to "RxJava缓存线程：RxJava库的线程池，用于异步操作",
        "FinalizerDaemon" to "终结器守护线程：处理对象终结化的系统线程",
        "ReferenceQueueDaemon" to "引用队列守护线程：处理弱引用、软引用的系统线程",
        "HeapTaskDaemon" to "堆任务守护线程：负责垃圾回收相关任务",
        "GC" to "垃圾回收线程：Java/Kotlin垃圾回收机制，清理未使用的内存",
        "RenderThread" to "渲染线程：负责UI渲染，属于Android图形系统",
        "GLThread" to "OpenGL线程：处理OpenGL图形绘制",
        "ExoPlayer" to "ExoPlayer线程：Google开发的媒体播放库线程",
        "Firebase" to "Firebase线程：Google Firebase服务相关线程",
        "OkHttp ConnectionPool" to "OkHttp连接池线程：管理HTTP连接复用",
        "Picasso" to "Picasso线程：Square公司开发的图片加载库线程",
        "Glide" to "Glide线程：Google推荐的图片加载库线程",
        "WorkManager" to "WorkManager线程：Android后台任务调度库线程",
        "kotlinx.coroutines" to "Kotlin协程线程：Kotlin语言的协程调度线程",
        "arch_disk_io" to "Architecture组件磁盘IO线程：Android架构组件的磁盘操作线程",
        "pool" to "线程池：通用线程池线程，用于执行多个后台任务"
    )

    @Composable
    override fun Content() {
        val threadVM = rememberScreenModel { ThreadViewModel() }
        val threads by threadVM.state.collectAsState()
        Column {
            summary(threads)
            Divider(thickness = 10.dp, color = Color.Transparent)
            tableScreen(threads)
        }
    }

    @Composable
    private fun summary(data: List<ThreadInfo>) {
        Text(
            "线程总数：${data.size}, Running：${data.count { it.state == "R" }}, Sleeping：${data.count { it.state == "S" }}",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium
        )
    }

    @Composable
    private fun tableScreen(data: List<ThreadInfo>) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(data) { threadInfo ->
                val (user, pid, tid, pPid, vsz, rss, wChan, address, state, cmd) = threadInfo
                val stateBg = if (state == "R") Color.Green else Color.Transparent
                
                // 为表头行添加详细说明
                if (threadInfo == data.firstOrNull()) {
                    TableRow(
                        Modifier.background(stateBg),
                        user.toTableRowItem(tooltip = columnTooltips["USER"] ?: ""),
                        pid.toTableRowItem(tooltip = columnTooltips["PID"] ?: ""),
                        tid.toTableRowItem(tooltip = columnTooltips["TID"] ?: ""),
                        pPid.toTableRowItem(tooltip = columnTooltips["PPID"] ?: ""),
                        vsz.toTableRowItem(tooltip = columnTooltips["VSZ"] ?: ""),
                        rss.toTableRowItem(tooltip = columnTooltips["RSS"] ?: ""),
                        wChan.toTableRowItem(tooltip = columnTooltips["WCHAN"] ?: ""),
                        address.toTableRowItem(tooltip = columnTooltips["ADDR"] ?: ""),
                        state.toTableRowItem(tooltip = columnTooltips["S"] ?: ""),
                        cmd.toTableRowItem(3f, tooltip = columnTooltips["CMD"] ?: "")
                    )
                } else {
                    // 解析cmd字符串，寻找线程名
                    val threadName = cmd.split(" ").lastOrNull() ?: ""
                    val threadTooltip = getThreadTooltip(threadName)
                    
                    // 格式化内存大小
                    val formattedVsz = FormatTool.formatMemorySize(vsz)
                    val formattedRss = FormatTool.formatMemorySize(rss)

                    TableRow(
                        Modifier.background(stateBg),
                        user.toTableRowItem(),
                        pid.toTableRowItem(),
                        tid.toTableRowItem(),
                        pPid.toTableRowItem(),
                        formattedVsz.toTableRowItem(tooltip = "虚拟内存：${formattedVsz}，原始值：${vsz} KB"),
                        formattedRss.toTableRowItem(tooltip = "常驻内存：${formattedRss}，原始值：${rss} KB"),
                        wChan.toTableRowItem(),
                        address.toTableRowItem(),
                        state.toTableRowItem(),
                        cmd.toTableRowItem(3f, tooltip = threadTooltip)
                    )
                }
            }
        }
    }
    
    // 获取线程的提示信息
    private fun getThreadTooltip(threadName: String): String {
        // 精确匹配
        if (threadTooltips.containsKey(threadName)) {
            return threadTooltips[threadName]!!
        }
        
        // 部分匹配，检查线程名是否包含已知线程名称
        for ((key, value) in threadTooltips) {
            if (threadName.contains(key, ignoreCase = true)) {
                return value
            }
        }
        
        // 默认提示
        return "线程名：$threadName"
    }
}