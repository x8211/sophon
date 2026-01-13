package sophon.desktop.feature.systemmonitor.feature.gfx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.systemmonitor.feature.gfx.domain.model.DisplayData
import sophon.desktop.feature.systemmonitor.feature.gfx.domain.model.GfxMetrics
import sophon.desktop.feature.systemmonitor.feature.gfx.domain.model.ViewRootInfo

// 配色方案 - 高对比度
private val PrimaryBlue = Color(0xFF2563EB)
private val PurpleAccent = Color(0xFF7C3AED)
private val OrangeWarning = Color(0xFFF97316)
private val RedError = Color(0xFFDC2626)
private val GreenSuccess = Color(0xFF16A34A)
private val GrayText = Color(0xFF6B7280)
private val LightGray = Color(0xFFF3F4F6)
private val WhiteBackground = Color(0xFFFFFFFF)

/**
 * 图形监测主屏幕
 * 优化版：简洁布局、白色背景、高对比度配色、单列全宽、整体滚动
 */
@Composable
fun GfxMonitorScreen(
    refreshTrigger: Long = 0,
    viewModel: GfxViewModel = viewModel { GfxViewModel() }
) {

    LaunchedEffect(refreshTrigger) {
        viewModel.refresh()
    }

    val displayData = viewModel.displayData
    val isRefreshing = viewModel.isRefreshing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGray)
    ) {
        if (displayData == null && isRefreshing) {
            LoadingView()
        } else if (displayData != null) {
            GfxContent(displayData)
        } else if (isRefreshing) {
            LoadingView()
        }
    }
}

/**
 * 加载视图
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = PrimaryBlue
            )
            Text(
                "正在获取 ADB 图形数据...",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayText
            )
        }
    }
}

/**
 * 主内容区域 - 整体滚动
 */
@Composable
private fun GfxContent(displayData: DisplayData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 全局汇总卡片
        item {
            GlobalSummaryCard(displayData)
        }

        // 窗口详情标题
        if (displayData.viewRootDetails.isNotEmpty()) {
            item {
                WindowsSectionHeader(displayData.viewRootDetails.size)
            }
        }

        // 窗口详情列表 - 单列全宽
        items(displayData.viewRootDetails) { viewRoot ->
            WindowCard(viewRoot)
        }
    }
}

/**
 * 全局汇总卡片 - 简化容器
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalSummaryCard(displayData: DisplayData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题和包名
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "全局性能汇总",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        displayData.packageName.ifEmpty { "未识别应用" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayText
                    )
                }

                // 掉帧率大数字
                JankPercentageBadge(displayData.globalMetrics.jankPercentage)
            }

            Divider(color = LightGray, thickness = 1.dp)

            // 指标网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 帧统计
                MetricItem(
                    icon = Icons.Default.BarChart,
                    iconColor = PrimaryBlue,
                    label = "总帧数",
                    value = displayData.globalMetrics.totalFrames.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    icon = Icons.Default.Error,
                    iconColor = RedError,
                    label = "掉帧数",
                    value = displayData.globalMetrics.jankyFrames.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    icon = Icons.Default.Window,
                    iconColor = PurpleAccent,
                    label = "窗口数",
                    value = displayData.totalViewRootImpl.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    icon = Icons.Default.Visibility,
                    iconColor = GreenSuccess,
                    label = "视图数",
                    value = displayData.totalViews.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = LightGray, thickness = 1.dp)

            // CPU/GPU 性能
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PerformanceColumn(
                    title = "CPU 耗时 (ms)",
                    color = PrimaryBlue,
                    metrics = displayData.globalMetrics,
                    isCpu = true,
                    modifier = Modifier.weight(1f)
                )
                PerformanceColumn(
                    title = "GPU 耗时 (ms)",
                    color = PurpleAccent,
                    metrics = displayData.globalMetrics,
                    isCpu = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // 性能瓶颈
            if (displayData.globalMetrics.jankReasons.isNotEmpty()) {
                Divider(color = LightGray, thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "性能瓶颈",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedError
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayData.globalMetrics.jankReasons.forEach { reason ->
                            ReasonTag(reason.description, reason.count)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 掉帧率徽章
 */
@Composable
private fun JankPercentageBadge(percentage: Float) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 800)
    )

    val color = when {
        percentage > 15f -> RedError
        percentage > 5f -> OrangeWarning
        else -> GreenSuccess
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${String.format("%.1f", animatedPercentage)}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                "掉帧率",
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
        }
    }
}

/**
 * 指标项
 */
@Composable
private fun MetricItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = GrayText
        )
    }
}

/**
 * 性能列
 */
@Composable
private fun PerformanceColumn(
    title: String,
    color: Color,
    metrics: GfxMetrics,
    isCpu: Boolean,
    modifier: Modifier = Modifier
) {
    val values = if (isCpu) {
        listOf(
            "P50" to metrics.p50Cpu,
            "P90" to metrics.p90Cpu,
            "P95" to metrics.p95Cpu,
            "P99" to metrics.p99Cpu
        )
    } else {
        listOf(
            "P50" to metrics.p50Gpu,
            "P90" to metrics.p90Gpu,
            "P95" to metrics.p95Gpu,
            "P99" to metrics.p99Gpu
        )
    }

    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(8.dp))
        values.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText
                )
                val valueColor = if (value > 16.7f) RedError else Color.Black
                Text(
                    String.format("%.1f", value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
            if (label != "P99") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 原因标签
 */
@Composable
private fun ReasonTag(description: String, count: Int) {
    Surface(
        color = RedError.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            "$description: $count",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = RedError,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 窗口区域标题
 */
@Composable
private fun WindowsSectionHeader(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PrimaryBlue)
        )
        Column {
            Text(
                "各窗口详细指标",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                "共 $count 个窗口",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText
            )
        }
    }
}

/**
 * 窗口卡片 - 单列全宽
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WindowCard(viewRoot: ViewRootInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    viewRoot.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                JankBadge(viewRoot.metrics.jankPercentage)
            }

            // 基本信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoBox(
                    icon = Icons.Default.Visibility,
                    label = "视图",
                    value = viewRoot.views.toString(),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                InfoBox(
                    icon = Icons.Default.Memory,
                    label = "显存",
                    value = viewRoot.renderNodeMemory,
                    color = PurpleAccent,
                    modifier = Modifier.weight(1f)
                )
                InfoBox(
                    icon = Icons.Default.CheckCircle,
                    label = "总帧",
                    value = viewRoot.metrics.totalFrames.toString(),
                    color = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                InfoBox(
                    icon = Icons.Default.Error,
                    label = "掉帧",
                    value = viewRoot.metrics.jankyFrames.toString(),
                    color = RedError,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = LightGray, thickness = 1.dp)

            // CPU/GPU 性能
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PerformanceColumn(
                    title = "CPU 耗时 (ms)",
                    color = PrimaryBlue,
                    metrics = viewRoot.metrics,
                    isCpu = true,
                    modifier = Modifier.weight(1f)
                )
                PerformanceColumn(
                    title = "GPU 耗时 (ms)",
                    color = PurpleAccent,
                    metrics = viewRoot.metrics,
                    isCpu = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // 性能瓶颈
            if (viewRoot.metrics.jankReasons.isNotEmpty()) {
                Divider(color = LightGray, thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "性能瓶颈",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedError
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewRoot.metrics.jankReasons.forEach { reason ->
                            ReasonTag(reason.description, reason.count)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 掉帧徽章
 */
@Composable
private fun JankBadge(percentage: Float) {
    val color = when {
        percentage > 15f -> RedError
        percentage > 5f -> OrangeWarning
        else -> GreenSuccess
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            "${String.format("%.1f", percentage)}%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 信息框
 */
@Composable
private fun InfoBox(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = GrayText,
                fontSize = 10.sp
            )
        }
    }
}
