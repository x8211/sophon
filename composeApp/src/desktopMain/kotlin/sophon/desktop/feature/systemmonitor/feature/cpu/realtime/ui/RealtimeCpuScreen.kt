package sophon.desktop.feature.systemmonitor.feature.cpu.realtime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.systemmonitor.feature.cpu.common.domain.model.ThreadCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeCpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeMemoryInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeProcessInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeSwapInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeSystemCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeTaskStats
import sophon.desktop.ui.theme.Dimens

/**
 * 实时CPU监测屏幕
 */
@Composable
fun RealtimeCpuScreen(
    refreshTrigger: Long = 0,
    viewModel: RealtimeCpuViewModel = viewModel { RealtimeCpuViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedProcessPid by viewModel.selectedProcessPid.collectAsState()
    val processThreads by viewModel.processThreads.collectAsState()
    val threadsLoading by viewModel.threadsLoading.collectAsState()
    val lastThreadUpdateTime by viewModel.lastThreadUpdateTime.collectAsState()

    // 监听刷新触发
    LaunchedEffect(refreshTrigger) {
        viewModel.refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(Dimens.paddingMedium)
    ) {
        when (val state = uiState) {
            is RealtimeCpuUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is RealtimeCpuUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重试")
                    }
                }
            }

            is RealtimeCpuUiState.Success -> RealtimeCpuContent(
                data = state.data,
                viewModel = viewModel
            )
        }
    }
    
    // 显示线程信息对话框（复用现有的ThreadListDialog）
    if (selectedProcessPid != null) {
        ThreadListDialog(
            pid = selectedProcessPid!!,
            threads = processThreads,
            isLoading = threadsLoading,
            lastUpdateTime = lastThreadUpdateTime,
            onDismiss = { viewModel.stopMonitoring() }
        )
    }
}

/**
 * 实时CPU监测内容
 */
@Composable
private fun RealtimeCpuContent(
    data: RealtimeCpuData,
    viewModel: RealtimeCpuViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerMedium)
    ) {
        // 任务统计卡片
        TaskStatsCard(data.taskStats)

        // 内存和Swap信息卡片
        MemoryInfoCard(data.memoryInfo, data.swapInfo)

        // 系统实时CPU信息
        RealtimeSystemCpuCard(data.systemCpu)

        // 进程列表
        RealtimeProcessListCard(
            processList = data.processList,
            onProcessClick = { process ->
                viewModel.startMonitoringProcessThreads(process.pid)
            }
        )
    }
}

/**
 * 任务统计卡片
 */
@Composable
private fun TaskStatsCard(taskStats: RealtimeTaskStats) {
    InfoCard(title = "任务统计 (Tasks)") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TaskMetricItem(
                label = "总数",
                value = taskStats.total,
                color = Color(0xFF2196F3)
            )
            TaskMetricItem(
                label = "运行中",
                value = taskStats.running,
                color = Color(0xFF4CAF50)
            )
            TaskMetricItem(
                label = "睡眠中",
                value = taskStats.sleeping,
                color = Color(0xFF9E9E9E)
            )
            TaskMetricItem(
                label = "已停止",
                value = taskStats.stopped,
                color = Color(0xFFFF9800)
            )
            TaskMetricItem(
                label = "僵尸",
                value = taskStats.zombie,
                color = Color(0xFFF44336)
            )
        }
    }
}

/**
 * 任务指标项
 */
@Composable
private fun TaskMetricItem(
    label: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 内存和Swap信息卡片
 */
@Composable
private fun MemoryInfoCard(memoryInfo: RealtimeMemoryInfo, swapInfo: RealtimeSwapInfo) {
    InfoCard(title = "内存与Swap信息") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 内存信息
            Text(
                text = "内存 (Memory)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MemoryMetricItem("总量", memoryInfo.total)
                MemoryMetricItem("已用", memoryInfo.used)
                MemoryMetricItem("空闲", memoryInfo.free)
                MemoryMetricItem("缓冲", memoryInfo.buffers)
            }
            
            HorizontalDivider()
            
            // Swap信息
            Text(
                text = "交换分区 (Swap)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MemoryMetricItem("总量", swapInfo.total)
                MemoryMetricItem("已用", swapInfo.used)
                MemoryMetricItem("空闲", swapInfo.free)
                MemoryMetricItem("缓存", swapInfo.cached)
            }
        }
    }
}

/**
 * 内存指标项
 */
@Composable
private fun MemoryMetricItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF424242)
        )
    }
}

/**
 * 实时系统CPU卡片
 */
@Composable
private fun RealtimeSystemCpuCard(systemCpu: RealtimeSystemCpuInfo) {
    InfoCard(title = "系统实时CPU使用率") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 总体CPU使用率 - 大号显示
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "实时CPU使用率",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.1f", systemCpu.getUsedPercent())}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = getCpuColor(systemCpu.getUsedPercent())
                )
                Text(
                    text = "总CPU容量: ${systemCpu.totalCpu.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            HorizontalDivider()

            // 详细分类
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CpuProgressItem(
                    label = "用户态 (User)",
                    value = systemCpu.userPercent,
                    maxValue = systemCpu.totalCpu,
                    description = "应用程序使用的CPU时间"
                )
                CpuProgressItem(
                    label = "Nice进程",
                    value = systemCpu.nicePercent,
                    maxValue = systemCpu.totalCpu,
                    description = "低优先级进程使用的CPU时间"
                )
                CpuProgressItem(
                    label = "系统态 (System)",
                    value = systemCpu.sysPercent,
                    maxValue = systemCpu.totalCpu,
                    description = "系统内核使用的CPU时间"
                )
                CpuProgressItem(
                    label = "空闲 (Idle)",
                    value = systemCpu.idlePercent,
                    maxValue = systemCpu.totalCpu,
                    description = "空闲的CPU时间",
                    isIdle = true
                )
                CpuProgressItem(
                    label = "IO等待 (IOWait)",
                    value = systemCpu.iowaitPercent,
                    maxValue = systemCpu.totalCpu,
                    description = "等待IO操作的CPU时间"
                )
                CpuProgressItem(
                    label = "硬中断 (IRQ)",
                    value = systemCpu.irqPercent,
                    maxValue = systemCpu.totalCpu,
                    description = "处理硬件中断的CPU时间"
                )
                CpuProgressItem(
                    label = "软中断 (SoftIRQ)",
                    value = systemCpu.softirqPercent,
                    maxValue = systemCpu.totalCpu,
                    description = "处理软件中断的CPU时间"
                )
                CpuProgressItem(
                    label = "主机 (Host)",
                    value = systemCpu.hostPercent,
                    maxValue = systemCpu.totalCpu,
                    description = "主机使用的CPU时间"
                )
            }
        }
    }
}

/**
 * CPU进度条项
 */
@Composable
private fun CpuProgressItem(
    label: String,
    value: Float,
    maxValue: Float,
    description: String,
    isIdle: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${String.format("%.1f", value)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIdle) Color(0xFF4CAF50) else getCpuColor(value)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isIdle) Color(0xFF4CAF50) else getCpuColor(value),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

/**
 * 根据CPU使用率获取颜色
 */
private fun getCpuColor(percent: Float): Color {
    return when {
        percent < 30f -> Color(0xFF4CAF50) // 绿色 - 正常
        percent < 60f -> Color(0xFFFF9800) // 橙色 - 中等
        else -> Color(0xFFF44336) // 红色 - 高负载
    }
}

/**
 * 实时进程列表卡片
 */
@Composable
private fun RealtimeProcessListCard(
    processList: List<RealtimeProcessInfo>,
    onProcessClick: (RealtimeProcessInfo) -> Unit
) {
    InfoCard(title = "实时进程CPU使用详情 (Top ${processList.size})") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 表头
            RealtimeProcessListHeader()

            HorizontalDivider()

            // 进程列表
            processList.forEach { process ->
                RealtimeProcessListItem(
                    process = process,
                    onClick = { onProcessClick(process) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "💡 点击进程可查看该进程的所有线程CPU使用情况",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 实时进程列表表头
 */
@Composable
private fun RealtimeProcessListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "进程信息",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = "状态",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = "CPU%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.7f)
        )
        Text(
            text = "内存%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.7f)
        )
        Text(
            text = "时间",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f)
        )
    }
}

/**
 * 实时进程列表项
 */
@Composable
private fun RealtimeProcessListItem(
    process: RealtimeProcessInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 进程信息
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = process.processName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "PID: ${process.pid} | ${process.user}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // 状态
        Text(
            text = getProcessStatusText(process.status),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = getProcessStatusColor(process.status),
            modifier = Modifier.weight(0.5f)
        )

        // CPU使用率
        Text(
            text = "${String.format("%.1f", process.cpuPercent)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = getCpuColor(process.cpuPercent),
            modifier = Modifier.weight(0.7f)
        )

        // 内存使用率
        Text(
            text = "${String.format("%.1f", process.memPercent)}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.7f)
        )

        // CPU时间
        Text(
            text = process.time,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.weight(0.8f)
        )
    }
}

/**
 * 获取进程状态文本
 */
private fun getProcessStatusText(status: String): String {
    return when (status) {
        "R" -> "运行"
        "S" -> "睡眠"
        "D" -> "等待"
        "Z" -> "僵尸"
        "T" -> "停止"
        else -> status
    }
}

/**
 * 获取进程状态颜色
 */
private fun getProcessStatusColor(status: String): Color {
    return when (status) {
        "R" -> Color(0xFF4CAF50) // 绿色 - 运行中
        "S" -> Color(0xFF9E9E9E) // 灰色 - 睡眠
        "D" -> Color(0xFFFF9800) // 橙色 - 等待
        "Z" -> Color(0xFFF44336) // 红色 - 僵尸
        "T" -> Color(0xFF2196F3) // 蓝色 - 停止
        else -> Color.Gray
    }
}

/**
 * 信息卡片
 */
@Composable
private fun InfoCard(
    title: String,
    backgroundColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 线程列表对话框
 * 复用现有的ThreadListDialog组件
 */
@Composable
private fun ThreadListDialog(
    pid: Int,
    threads: List<ThreadCpuInfo>,
    isLoading: Boolean,
    lastUpdateTime: Long,
    onDismiss: () -> Unit
) {
    // 按CPU使用率降序排序
    val sortedThreads = threads.sortedByDescending { it.totalPercent }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .width(900.dp)
                .height(700.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "进程线程详情",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PID: $pid",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "🔄 持续监测中",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (lastUpdateTime > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "最后更新: ${formatUpdateTime(lastUpdateTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.width(32.dp).height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(48.dp).height(48.dp),
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = "正在加载线程信息...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else if (sortedThreads.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📭",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "未找到线程信息",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "该进程可能已结束或无法访问",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                } else {
                    // 统计信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🧵",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Column {
                                    Text(
                                        text = "线程总数",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${sortedThreads.size} 个线程",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "已按CPU使用率排序 ↓",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "每2秒自动刷新",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 线程列表
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 表头
                            ThreadListHeader()
                            
                            HorizontalDivider(thickness = 2.dp, color = Color(0xFFE0E0E0))
                            
                            // 线程列表（可滚动）
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sortedThreads.forEach { thread ->
                                    ThreadListItem(thread)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 线程列表表头
 */
@Composable
private fun ThreadListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1976D2).copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "线程名称",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(2f)
        )
        Text(
            text = "TID",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "总CPU",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "用户态",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "内核态",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(0.8f)
        )
    }
}

/**
 * 线程列表项
 */
@Composable
private fun ThreadListItem(thread: ThreadCpuInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 线程名称
            Text(
                text = thread.threadName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                modifier = Modifier.weight(2f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            // TID
            Text(
                text = thread.tid.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF757575),
                modifier = Modifier.weight(0.8f)
            )
            
            // 总CPU使用率
            Text(
                text = "${String.format("%.1f", thread.totalPercent)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = getCpuColor(thread.totalPercent),
                modifier = Modifier.weight(0.8f)
            )
            
            // 用户态
            Text(
                text = "${String.format("%.1f", thread.userPercent)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF757575),
                modifier = Modifier.weight(0.8f)
            )
            
            // 内核态
            Text(
                text = "${String.format("%.1f", thread.kernelPercent)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF757575),
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

/**
 * 格式化更新时间
 */
private fun formatUpdateTime(timestamp: Long): String {
    if (timestamp == 0L) return "未更新"
    
    val now = System.currentTimeMillis()
    val diffSeconds = (now - timestamp) / 1000
    
    return when {
        diffSeconds < 5 -> "刚刚"
        diffSeconds < 60 -> "${diffSeconds}秒前"
        diffSeconds < 3600 -> "${diffSeconds / 60}分钟前"
        else -> "${diffSeconds / 3600}小时前"
    }
}
