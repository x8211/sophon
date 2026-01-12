package sophon.desktop.feature.cpumonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import sophon.desktop.feature.cpumonitor.domain.model.CpuLoadInfo
import sophon.desktop.feature.cpumonitor.domain.model.CpuMonitorData
import sophon.desktop.feature.cpumonitor.domain.model.CpuTimeRange
import sophon.desktop.feature.cpumonitor.domain.model.ProcessCpuInfo
import sophon.desktop.feature.cpumonitor.domain.model.SystemCpuInfo
import sophon.desktop.ui.theme.Dimens

/**
 * CPU监测屏幕
 */
@Composable
fun CpuMonitorScreen(viewModel: CpuMonitorViewModel = viewModel { CpuMonitorViewModel() }) {
    val uiState by viewModel.uiState.collectAsState()

    // 启动监测 - 默认监控所有进程
    LaunchedEffect(Unit) {
        viewModel.startMonitoring(packageName = null)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(Dimens.paddingMedium)
    ) {
        when (val state = uiState) {
            is CpuMonitorUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is CpuMonitorUiState.Error -> {
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

            is CpuMonitorUiState.Success -> {
                CpuMonitorContent(
                    data = state.data,
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    }
}

/**
 * CPU监测内容
 */
@Composable
private fun CpuMonitorContent(
    data: CpuMonitorData,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerMedium)
    ) {
        // CPU负载信息卡片
        CpuLoadCard(data.loadInfo)

        // 时间范围卡片
        TimeRangeCard(data.timeRange)

        // 目标应用CPU信息(如果有)
        data.targetProcess?.let { targetProcess ->
            TargetProcessCard(targetProcess)
        }

        // 系统整体CPU信息
        SystemCpuCard(data.systemCpu)

        // 进程列表
        ProcessListCard(data.processList)
    }
}

/**
 * CPU负载信息卡片
 */
@Composable
private fun CpuLoadCard(loadInfo: CpuLoadInfo) {
    InfoCard(title = "系统负载 (Load Average)") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LoadMetricItem(
                label = "1分钟",
                value = loadInfo.load1min,
                description = "最近1分钟的平均负载"
            )
            LoadMetricItem(
                label = "5分钟",
                value = loadInfo.load5min,
                description = "最近5分钟的平均负载"
            )
            LoadMetricItem(
                label = "15分钟",
                value = loadInfo.load15min,
                description = "最近15分钟的平均负载"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 负载说明
        Text(
            text = "💡 负载值表示等待CPU处理的进程数量。一般来说,负载值低于CPU核心数表示系统运行正常。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 负载指标项
 */
@Composable
private fun LoadMetricItem(
    label: String,
    value: Float,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%.2f", value),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = getLoadColor(value)
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
 * 根据负载值获取颜色
 */
private fun getLoadColor(load: Float): Color {
    return when {
        load < 2f -> Color(0xFF4CAF50) // 绿色 - 正常
        load < 5f -> Color(0xFFFF9800) // 橙色 - 警告
        else -> Color(0xFFF44336) // 红色 - 高负载
    }
}

/**
 * 时间范围卡片
 */
@Composable
private fun TimeRangeCard(timeRange: CpuTimeRange) {
    InfoCard(title = "统计时间范围") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow(label = "开始时间", value = timeRange.startTime)
            InfoRow(label = "结束时间", value = timeRange.endTime)
            InfoRow(
                label = "统计时长",
                value = "${timeRange.durationMs}ms (${
                    String.format(
                        "%.2f",
                        timeRange.durationMs / 1000.0
                    )
                }秒)"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "💡 CPU使用率是在此时间段内统计的平均值",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 目标应用进程卡片
 */
@Composable
private fun TargetProcessCard(process: ProcessCpuInfo) {
    InfoCard(
        title = "当前应用CPU使用情况",
        backgroundColor = Color(0xFFE3F2FD)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow(label = "进程名称", value = process.processName, valueColor = Color(0xFF1976D2))
            InfoRow(label = "进程ID (PID)", value = process.pid.toString())

            Spacer(modifier = Modifier.height(8.dp))

            // CPU使用率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CpuMetricItem(
                    label = "总CPU",
                    value = process.totalPercent,
                    description = "总体CPU占用率"
                )
                CpuMetricItem(
                    label = "用户态",
                    value = process.userPercent,
                    description = "用户空间CPU占用"
                )
                CpuMetricItem(
                    label = "内核态",
                    value = process.kernelPercent,
                    description = "内核空间CPU占用"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 页错误信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FaultMetricItem(
                    label = "次要页错误",
                    value = process.minorFaults,
                    description = "Minor Faults - 可从内存恢复"
                )
                FaultMetricItem(
                    label = "主要页错误",
                    value = process.majorFaults,
                    description = "Major Faults - 需从磁盘加载"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "💡 用户态CPU主要用于应用逻辑,内核态CPU用于系统调用。页错误表示内存访问需要额外处理。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 系统整体CPU卡片
 */
@Composable
private fun SystemCpuCard(systemCpu: SystemCpuInfo) {
    InfoCard(title = "系统整体CPU使用率") {
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
                    text = "总体CPU使用率",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.1f", systemCpu.totalPercent)}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = getCpuColor(systemCpu.totalPercent)
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
                    description = "应用程序使用的CPU时间"
                )
                CpuProgressItem(
                    label = "内核态 (Kernel)",
                    value = systemCpu.kernelPercent,
                    description = "系统内核使用的CPU时间"
                )
                CpuProgressItem(
                    label = "IO等待 (IOWait)",
                    value = systemCpu.iowaitPercent,
                    description = "等待IO操作的CPU时间"
                )
                CpuProgressItem(
                    label = "硬中断 (IRQ)",
                    value = systemCpu.irqPercent,
                    description = "处理硬件中断的CPU时间"
                )
                CpuProgressItem(
                    label = "软中断 (SoftIRQ)",
                    value = systemCpu.softirqPercent,
                    description = "处理软件中断的CPU时间"
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
    description: String
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
                color = getCpuColor(value)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = getCpuColor(value),
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
 * CPU指标项
 */
@Composable
private fun CpuMetricItem(
    label: String,
    value: Float,
    description: String
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
            text = "${String.format("%.1f", value)}%",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = getCpuColor(value)
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
 * 页错误指标项
 */
@Composable
private fun FaultMetricItem(
    label: String,
    value: Int,
    description: String
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
            color = Color(0xFF2196F3)
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
 * 进程列表卡片
 */
@Composable
private fun ProcessListCard(processList: List<ProcessCpuInfo>) {
    InfoCard(title = "所有进程CPU使用详情 (共${processList.size}个进程)") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 表头
            ProcessListHeader()

            HorizontalDivider()

            // 进程列表
            processList.forEach { process ->
                ProcessListItem(process)
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}

/**
 * 进程列表表头
 */
@Composable
private fun ProcessListHeader() {
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
            text = "总CPU",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "用户态",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "内核态",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "页错误",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 进程列表项
 */
@Composable
private fun ProcessListItem(process: ProcessCpuInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 进程信息
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = process.processName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "PID: ${process.pid}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // 总CPU
        Text(
            text = "${String.format("%.1f", process.totalPercent)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = getCpuColor(process.totalPercent),
            modifier = Modifier.weight(0.8f)
        )

        // 用户态
        Text(
            text = "${String.format("%.1f", process.userPercent)}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.8f)
        )

        // 内核态
        Text(
            text = "${String.format("%.1f", process.kernelPercent)}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.8f)
        )

        // 页错误
        Column(modifier = Modifier.weight(1f)) {
            if (process.minorFaults > 0 || process.majorFaults > 0) {
                Text(
                    text = "Minor: ${process.minorFaults}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (process.majorFaults > 0) {
                    Text(
                        text = "Major: ${process.majorFaults}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5722)
                    )
                }
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
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
