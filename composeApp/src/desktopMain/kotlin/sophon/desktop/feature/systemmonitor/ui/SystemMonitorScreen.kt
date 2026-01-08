package sophon.desktop.feature.systemmonitor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.systemmonitor.domain.model.MonitorDataPoint
import sophon.desktop.ui.theme.Dimens
import kotlin.math.max

/**
 * 系统监测主屏幕
 * 展示温度和屏幕帧率监测数据的折线图
 */
@Composable
fun SystemMonitorScreen(
    viewModel: SystemMonitorViewModel = viewModel { SystemMonitorViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val visibleMonitors by viewModel.visibleMonitors.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(Dimens.paddingLarge)
    ) {
        // 多选控制器 (取代标题栏)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.cornerRadiusMedium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.paddingMedium, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.paddingLarge)
            ) {
                Text("显示选项:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                
                MonitorCheckbox(
                    label = "温度监测",
                    checked = visibleMonitors.contains("Temperature"),
                    onCheckedChange = { viewModel.toggleMonitorVisibility("Temperature") }
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacerMedium))

        // 温度监测卡片
        AnimatedVisibility(
            visible = visibleMonitors.contains("Temperature"),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                MonitorCard(
                    title = "温度监测",
                    icon = Icons.Default.Thermostat,
                    iconColor = Color(0xFFFF6B6B),
                    currentValue = uiState.temperatureData.currentTemp,
                    unit = "°C",
                    maxValue = uiState.temperatureData.maxTemp,
                    minValue = uiState.temperatureData.minTemp,
                    avgValue = uiState.temperatureData.avgTemp,
                    dataPoints = uiState.temperatureData.dataPoints,
                    lineColor = Color(0xFFFF6B6B)
                )
                Spacer(modifier = Modifier.height(Dimens.spacerMedium))
            }
        }
        Spacer(modifier = Modifier.height(Dimens.paddingLarge))
    }
}

/**
 * 监测项显示控制多选框
 */
@Composable
private fun MonitorCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 监测卡片组件
 * 展示单个监测项的数据和折线图
 */
@Composable
private fun MonitorCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    currentValue: Float,
    unit: String,
    maxValue: Float,
    minValue: Float,
    avgValue: Float,
    dataPoints: List<MonitorDataPoint>,
    lineColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium)
        ) {
            // 标题与实时值行 - 更加紧凑
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color = iconColor.copy(alpha = 0.1f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = String.format("%.1f", currentValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = lineColor)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacerSmall))

            // 统计信息调整为一行紧凑显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.paddingLarge)
            ) {
                StatisticTinyItem("Max", String.format("%.1f", maxValue), Color(0xFFE74C3C))
                StatisticTinyItem("Min", String.format("%.1f", minValue), Color(0xFF3498DB))
                StatisticTinyItem("Avg", String.format("%.1f", avgValue), Color(0xFF2ECC71))
            }

            Spacer(modifier = Modifier.height(Dimens.spacerSmall))

            // 折线图高度减小并加入坐标轴
            if (dataPoints.isNotEmpty()) {
                LineChart(
                    dataPoints = dataPoints,
                    lineColor = lineColor,
                    unit = unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // 高度从200减小到140
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text(text = "等待采集数据...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * 紧凑型统计项
 */
@Composable
private fun StatisticTinyItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * 折线图组件 (加入坐标轴显示)
 */
@Composable
private fun LineChart(
    dataPoints: List<MonitorDataPoint>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "chartAnimation"
    )

    val labelStyle = TextStyle(color = Color.Gray, fontSize = 10.sp)

    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val leftPadding = 60f  // 为左侧纵轴标签留出空间
        val bottomPadding = 40f // 为底部横轴标签留出空间
        val topPadding = 20f
        val rightPadding = 20f

        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding

        // 数据计算
        val values = dataPoints.map { it.value }
        val maxValue = values.maxOrNull() ?: 1f
        val minValue = values.minOrNull() ?: 0f
        val valueRange = max(maxValue - minValue, 0.1f)

        // 1. 绘制纵轴文字 (Max, Min, Middle)
        val labels = listOf(
            maxValue to topPadding,
            (maxValue + minValue) / 2 to topPadding + chartHeight / 2,
            minValue to topPadding + chartHeight
        )
        
        labels.forEach { (value, y) ->
            val text = String.format("%.1f", value)
            val measuredText = textMeasurer.measure(text, labelStyle)
            drawText(
                textLayoutResult = measuredText,
                topLeft = Offset(leftPadding - measuredText.size.width.toFloat() - 10f, y - measuredText.size.height.toFloat() / 2)
            )
            
            // 绘制对应的辅助线
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(leftPadding, y),
                end = Offset(width - rightPadding, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // 2. 绘制横轴文字 (模拟时间标注)
        val timeLabels = listOf("-60s" to 0f, "-30s" to 0.5f, "现在" to 1f)
        timeLabels.forEach { (text, ratio) ->
            val measuredText = textMeasurer.measure(text, labelStyle)
            drawText(
                textLayoutResult = measuredText,
                topLeft = Offset(leftPadding + chartWidth * ratio - measuredText.size.width.toFloat() / 2, height - bottomPadding + 5f)
            )
        }

        // 3. 计算数据点位置
        val points = dataPoints.mapIndexed { index, point ->
            val x = leftPadding + chartWidth * index / max(dataPoints.size - 1, 1)
            val normalizedValue = (point.value - minValue) / valueRange
            val y = topPadding + chartHeight * (1f - normalizedValue)
            Offset(x, y)
        }

        val animatedPoints = points.take((points.size * animationProgress).toInt().coerceAtLeast(1))
        if (animatedPoints.size < 2) return@Canvas

        // 4. 绘制填充
        val gradientPath = Path().apply {
            moveTo(animatedPoints.first().x, topPadding + chartHeight)
            lineTo(animatedPoints.first().x, animatedPoints.first().y)
            for (i in 0 until animatedPoints.size - 1) {
                val p0 = animatedPoints[i]
                val p1 = animatedPoints[i + 1]
                val controlX = (p0.x + p1.x) / 2
                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }
            lineTo(animatedPoints.last().x, topPadding + chartHeight)
            close()
        }

        drawPath(
            path = gradientPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), lineColor.copy(alpha = 0.01f)),
                startY = topPadding,
                endY = topPadding + chartHeight
            )
        )

        // 5. 绘制曲线
        val linePath = Path().apply {
            moveTo(animatedPoints.first().x, animatedPoints.first().y)
            for (i in 0 until animatedPoints.size - 1) {
                val p0 = animatedPoints[i]
                val p1 = animatedPoints[i + 1]
                val controlX = (p0.x + p1.x) / 2
                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }
}
