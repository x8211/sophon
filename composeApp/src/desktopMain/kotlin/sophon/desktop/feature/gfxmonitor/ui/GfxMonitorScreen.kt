package sophon.desktop.feature.gfxmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData
import sophon.desktop.feature.gfxmonitor.domain.model.ViewRootInfo
import sophon.desktop.ui.theme.Dimens

/**
 * 图形监测主屏幕
 * 核心优化版：聚合汇总信息，详情全宽展示
 */
@Composable
fun GfxMonitorScreen() {
    val viewModel = remember { GfxMonitorViewModel() }

    DisposableEffect(Unit) {
        viewModel.startMonitoring()
        onDispose {
            viewModel.onCleared()
        }
    }

    val displayData = viewModel.displayData

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (displayData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(Dimens.spacerSmall))
                    Text("正在获取 ADB 图形数据...", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            GfxContent(displayData)
        }
    }
}

@Composable
private fun GfxContent(displayData: DisplayData) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1), // 全宽排列
        modifier = Modifier.fillMaxSize().padding(Dimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.paddingLarge)
    ) {
        // 1. 汇总概览块
        item {
            GfxOverviewCard(displayData)
        }

        // 2. 详情标题
        if (displayData.viewRootDetails.isNotEmpty()) {
            item {
                Text(
                    "窗口详情 (${displayData.viewRootDetails.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Dimens.paddingMedium)
                )
            }

            // 3. 各窗口详情卡片
            items(displayData.viewRootDetails) { viewRoot ->
                ViewRootDetailCard(viewRoot)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GfxOverviewCard(displayData: DisplayData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.paddingLarge)) {
            // 第一部分：应用包名与掉帧率核心指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = displayData.packageName.ifEmpty { "未识别当前应用" },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.paddingSmall))
                    Text(
                        "图形性能汇总记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val jankColor = getJankColor(displayData.globalMetrics.jankPercentage)
                    Text(
                        text = "${String.format("%.2f", displayData.globalMetrics.jankPercentage)}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = jankColor
                    )
                    Text(
                        "掉帧率 (Janky Frames)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacerMedium))
            HorizontalDivider(modifier = Modifier.alpha(0.2f))
            Spacer(modifier = Modifier.height(Dimens.spacerMedium))

            // 第二部分：具体性能指标网格 (耗时分位值 + 视图统计)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // 耗时详情
                Column(modifier = Modifier.weight(1.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("渲染耗时分位值 (ms)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MetricColumn("CPU", listOf(
                            "P50" to displayData.globalMetrics.p50Cpu,
                            "P90" to displayData.globalMetrics.p90Cpu,
                            "P95" to displayData.globalMetrics.p95Cpu,
                            "P99" to displayData.globalMetrics.p99Cpu
                        ), Modifier.weight(1f))
                        MetricColumn("GPU", listOf(
                            "P50" to displayData.globalMetrics.p50Gpu,
                            "P90" to displayData.globalMetrics.p90Gpu,
                            "P95" to displayData.globalMetrics.p95Gpu,
                            "P99" to displayData.globalMetrics.p99Gpu
                        ), Modifier.weight(1f))
                    }
                }

                // 结构统计
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("视图结构与内存", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OverviewSummaryItem("总窗口数", displayData.totalViewRootImpl.toString(), Icons.Default.AccountTree)
                        OverviewSummaryItem("总视图数", displayData.totalViews.toString(), Icons.Default.Memory)
                        
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("显存: ${displayData.renderNodeUsedMemory}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("总: ${displayData.renderNodeCapacityMemory}", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { 0.5f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // 第三部分：全局原因分类
            if (displayData.globalMetrics.jankReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacerMedium))
                HorizontalDivider(modifier = Modifier.alpha(0.2f))
                Spacer(modifier = Modifier.height(Dimens.spacerSmall))
                Text("全局性能瓶颈:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayData.globalMetrics.jankReasons.forEach { reason ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${reason.description}: ${reason.count}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSummaryItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun MetricColumn(title: String, values: List<Pair<String, Float>>, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        values.forEach { (name, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                val color = if (value > 16.7f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                Text("${value}ms", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewRootDetailCard(viewRoot: ViewRootInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Dimens.paddingLarge)) {
            // Header: Name and Jank%
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        viewRoot.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "附着视图: ${viewRoot.views} | 显存占用: ${viewRoot.renderNodeMemory}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val jankColor = getJankColor(viewRoot.metrics.jankPercentage)
                    Surface(
                        color = jankColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "${String.format("%.1f", viewRoot.metrics.jankPercentage)}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = jankColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacerMedium))
            HorizontalDivider(modifier = Modifier.alpha(0.1f))
            Spacer(modifier = Modifier.height(Dimens.spacerMedium))

            // Metrics: Detailed Percentiles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MetricColumn("CPU Percentiles", listOf(
                    "P50" to viewRoot.metrics.p50Cpu,
                    "P90" to viewRoot.metrics.p90Cpu,
                    "P95" to viewRoot.metrics.p95Cpu,
                    "P99" to viewRoot.metrics.p99Cpu
                ), Modifier.weight(1f))
                MetricColumn("GPU Percentiles", listOf(
                    "P50" to viewRoot.metrics.p50Gpu,
                    "P90" to viewRoot.metrics.p90Gpu,
                    "P95" to viewRoot.metrics.p95Gpu,
                    "P99" to viewRoot.metrics.p99Gpu
                ), Modifier.weight(1f))
            }
            
            // Stats Footer
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "总渲染帧: ${viewRoot.metrics.totalFrames}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "掉帧数: ${viewRoot.metrics.jankyFrames}",
                    style = MaterialTheme.typography.labelSmall,
                    color = getJankColor(viewRoot.metrics.jankPercentage)
                )
            }

            // Reasons
            if (viewRoot.metrics.jankReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacerSmall))
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                Spacer(modifier = Modifier.height(Dimens.spacerSmall))
                Text(
                    "当前窗口瓶颈识别:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    viewRoot.metrics.jankReasons.forEach { reason ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${reason.description}: ${reason.count}",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getJankColor(percentage: Float): Color {
    return when {
        percentage > 15f -> Color(0xFFE74C3C)
        percentage > 5f -> Color(0xFFF39C12)
        else -> Color(0xFF27AE60)
    }
}
