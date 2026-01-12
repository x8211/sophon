package sophon.desktop.feature.thread.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.common.util.FormatTool
import sophon.desktop.feature.thread.domain.model.ProcessInfo
import sophon.desktop.feature.thread.domain.model.ThreadInfo

// 常见线程描述信息 - 尽可能全面覆盖Android常见线程
private val threadDescriptions = mapOf(
    // 主线程和UI相关
    "main" to "主线程 - 应用程序的UI线程，负责处理用户交互和界面绘制",
    "UI Thread" to "UI线程 - 处理用户界面更新和事件",
    "RenderThread" to "渲染线程 - 负责UI渲染，属于Android图形系统",
    "GLThread" to "OpenGL线程 - 处理OpenGL图形绘制",
    "hwuiTask" to "硬件UI任务线程 - 硬件加速渲染任务",

    // Binder和IPC相关
    "Binder" to "Binder线程 - Android的IPC通信机制，用于进程间通信",
    "binder" to "Binder工作线程 - 处理跨进程调用",

    // 内存管理和GC相关
    "FinalizerDaemon" to "终结器守护线程 - 处理对象终结化的系统线程",
    "FinalizerWatchdogDaemon" to "终结器监控线程 - 监控终结器执行时间",
    "ReferenceQueueDaemon" to "引用队列守护线程 - 处理弱引用、软引用的系统线程",
    "HeapTaskDaemon" to "堆任务守护线程 - 负责垃圾回收相关任务",
    "GC" to "垃圾回收线程 - Java/Kotlin垃圾回收机制，清理未使用的内存",
    "FinalizerHelper" to "终结器辅助线程 - 协助对象终结化",

    // 异步任务相关
    "AsyncTask" to "异步任务线程 - 用于后台操作，避免阻塞主线程",
    "pool" to "线程池 - 通用线程池线程，用于执行多个后台任务",
    "Thread" to "工作线程 - 执行后台任务",

    // 网络相关
    "OkHttp" to "OkHttp网络库线程 - 处理HTTP网络请求",
    "OkHttp ConnectionPool" to "OkHttp连接池线程 - 管理HTTP连接复用",
    "OkHttp Dispatcher" to "OkHttp调度线程 - 调度网络请求",
    "Okio Watchdog" to "Okio监控线程 - 监控IO超时",

    // RxJava相关
    "RxCachedThreadScheduler" to "RxJava缓存线程 - RxJava库的线程池，用于异步操作",
    "RxComputationScheduler" to "RxJava计算线程 - 用于CPU密集型计算",
    "RxIoScheduler" to "RxJava IO线程 - 用于IO密集型操作",
    "RxNewThreadScheduler" to "RxJava新线程调度器 - 为每个任务创建新线程",
    "RxSingleScheduler" to "RxJava单线程调度器 - 单一后台线程",

    // Kotlin协程相关
    "kotlinx.coroutines" to "Kotlin协程线程 - Kotlin语言的协程调度线程",
    "DefaultDispatcher" to "协程默认调度器 - 用于CPU密集型任务",
    "CommonPool" to "协程公共池 - 共享的协程线程池",

    // 图片加载库相关
    "Picasso" to "Picasso线程 - Square公司开发的图片加载库线程",
    "Glide" to "Glide线程 - Google推荐的图片加载库线程",
    "Fresco" to "Fresco线程 - Facebook开发的图片加载库线程",
    "Coil" to "Coil线程 - Kotlin优先的图片加载库线程",

    // 媒体相关
    "ExoPlayer" to "ExoPlayer线程 - Google开发的媒体播放库线程",
    "MediaCodec" to "媒体编解码线程 - 处理音视频编解码",
    "AudioTrack" to "音频轨道线程 - 音频播放",
    "Camera" to "相机线程 - 相机操作和预览",

    // Firebase和Google服务相关
    "Firebase" to "Firebase线程 - Google Firebase服务相关线程",
    "Firebase-Messaging" to "Firebase消息线程 - 处理推送消息",
    "Firebase-Database" to "Firebase数据库线程 - 实时数据库操作",
    "GoogleApiHandler" to "Google API处理线程 - Google服务API调用",

    // Android架构组件相关
    "arch_disk_io" to "架构组件磁盘IO线程 - Android架构组件的磁盘操作",
    "arch_network_io" to "架构组件网络IO线程 - 网络操作",
    "WorkManager" to "WorkManager线程 - Android后台任务调度库线程",
    "LiveData" to "LiveData线程 - 数据观察和更新",
    "Room" to "Room数据库线程 - 数据库操作",

    // 数据库相关
    "SQLiteThread" to "SQLite线程 - SQLite数据库操作",
    "Realm" to "Realm数据库线程 - Realm数据库操作",
    "GreenDao" to "GreenDao线程 - GreenDao数据库操作",

    // 传感器和定位相关
    "SensorService" to "传感器服务线程 - 处理设备传感器数据",
    "LocationManager" to "定位管理线程 - GPS和网络定位",
    "FusedLocation" to "融合定位线程 - Google融合定位服务",

    // 文件和IO相关
    "FileObserver" to "文件观察线程 - 监控文件系统变化",
    "DiskIO" to "磁盘IO线程 - 文件读写操作",
    "Download" to "下载线程 - 文件下载任务",

    // 系统服务相关
    "PackageManager" to "包管理线程 - 应用包管理操作",
    "ActivityManager" to "活动管理线程 - Activity生命周期管理",
    "InputDispatcher" to "输入分发线程 - 触摸和按键事件分发",
    "InputReader" to "输入读取线程 - 读取输入设备事件",

    // 其他常见线程
    "Timer" to "定时器线程 - 执行定时任务",
    "Handler" to "Handler线程 - 消息处理",
    "HandlerThread" to "Handler线程 - 带消息循环的工作线程",
    "IntentService" to "Intent服务线程 - 后台服务任务",
    "JobScheduler" to "任务调度线程 - 系统任务调度",
    "AlarmManager" to "闹钟管理线程 - 定时任务触发",
    "Choreographer" to "编舞者线程 - 协调动画和绘制时机",
    "perfetto" to "性能追踪线程 - 系统性能监控",
    "Profile Saver" to "配置保存线程 - 保存应用配置信息",
    "Signal Catcher" to "信号捕获线程 - 捕获系统信号",
    "JDWP" to "JDWP调试线程 - Java调试线协议",
    "Daemon" to "守护线程 - 后台守护进程"
)

/**
 * 线程信息主界面
 */
@Composable
fun ThreadScreen(viewModel: ThreadViewModel = viewModel { ThreadViewModel() }) {

    // 启动监测
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    val processInfo by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (processInfo != null) {
            ProcessInfoCard(processInfo!!)
            ThreadListSection(processInfo!!)
        } else {
            EmptyStateView()
        }
    }
}

/**
 * 进程信息卡片
 */
@Composable
private fun ProcessInfoCard(processInfo: ProcessInfo) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题
            Text(
                text = "进程信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            // 进程详细信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoItem("包名", processInfo.packageName.ifEmpty { "未知" })
                    InfoItem("用户", processInfo.user)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoItem("虚拟内存", FormatTool.formatMemorySize(processInfo.vsz))
                    InfoItem("常驻内存", FormatTool.formatMemorySize(processInfo.rss))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoItem("进程ID", processInfo.pid)
                    InfoItem("父进程ID", processInfo.ppid)
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            // 线程统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticChip(
                    label = "总线程数",
                    value = processInfo.getTotalThreadCount().toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatisticChip(
                    label = "运行中",
                    value = processInfo.getRunningThreadCount().toString(),
                    color = Color(0xFF4CAF50)
                )
                StatisticChip(
                    label = "睡眠中",
                    value = processInfo.getSleepingThreadCount().toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * 统计芯片
 */
@Composable
private fun StatisticChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 线程列表区域
 */
@Composable
private fun ThreadListSection(processInfo: ProcessInfo) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 线程表格
        ElevatedCard(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 表头
                ThreadTableHeader()

                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)

                // 线程列表
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(processInfo.threads) { thread ->
                        ThreadTableRow(processInfo.pid, thread)
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 线程表格表头
 */
@Composable
private fun ThreadTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeaderCell("TID", 0.6f)
        HeaderCell("状态", 0.4f)
        HeaderCell("等待通道", 0.8f)
        HeaderCell("命令/名称", 2.2f)
        HeaderCell("线程作用", 2f)
    }
}

/**
 * 表头单元格
 */
@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * 线程表格行
 */
@Composable
private fun ThreadTableRow(pid: String, thread: ThreadInfo) {
    val backgroundColor = when (thread.state) {
        "R" -> Color(0xFF4CAF50).copy(alpha = 0.15f)  // 运行中 - 绿色背景
        "S" -> Color.Transparent
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    }

    val threadDescription = getThreadDescription(thread.tid == pid, thread.cmd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DataCell(thread.tid, 0.6f)
        StateCell(thread.state, 0.4f)
        DataCell(thread.wchan, 0.8f)
        DataCell(thread.cmd, 2.2f)
        DataCell(threadDescription, 2f)
    }
}

/**
 * 数据单元格
 */
@Composable
private fun RowScope.DataCell(text: String, weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 状态单元格(带颜色标识)
 */
@Composable
private fun RowScope.StateCell(state: String, weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = when (state) {
                "R" -> Color(0xFF4CAF50)
                "S" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        ) {
            Text(
                text = state,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "暂无进程信息",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "请确保已连接设备并有前台应用运行",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 获取线程的提示信息
 */
/**
 * 获取线程的描述信息
 */
private fun getThreadDescription(isMainThread: Boolean, threadName: String): String {
    // 精确匹配
    if (threadDescriptions.containsKey(threadName)) {
        return threadDescriptions[threadName]!!
    }

    // 部分匹配，检查线程名是否包含已知线程名称
    for ((key, value) in threadDescriptions) {
        if (threadName.contains(key, ignoreCase = true)) {
            return value
        }
    }

    if (isMainThread) return "主线程"

    // 默认描述
    return "未知线程类型"
}
