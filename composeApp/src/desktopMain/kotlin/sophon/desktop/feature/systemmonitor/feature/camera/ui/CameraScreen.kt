package sophon.desktop.feature.systemmonitor.feature.camera.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraData
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraDeviceInfo
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraEventLog
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraEventType
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraStream
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.DataSpaceHelper
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.UsageHelper
import sophon.desktop.ui.theme.Dimens

/**
 * 相机监控屏幕
 *
 * 显示相机服务状态，包括每个相机设备的详细信息和事件日志
 *
 * @param refreshTrigger 刷新触发器，由父级控制
 * @param viewModel 相机监控 ViewModel
 */
@Composable
fun CameraScreen(
    refreshTrigger: Long = 0,
    viewModel: CameraViewModel = viewModel { CameraViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

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
            is CameraUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is CameraUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is CameraUiState.Success -> {
                CameraContent(data = state.data)
            }
        }
    }
}

/**
 * 错误状态内容
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("重试")
        }
    }
}

/**
 * 相机监控主内容
 *
 * 仅显示设备卡片和事件日志
 */
@Composable
private fun CameraContent(data: CameraData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerMedium)
    ) {
        // 1. 设备动态信息卡片
        data.deviceInfoList.forEach { deviceInfo ->
            DeviceInfoCard(deviceInfo)
        }

        // 2. 事件日志卡片 (仅显示最近20条)
        if (data.eventLogs.isNotEmpty()) {
            EventLogsCard(data.eventLogs.take(20))
        }
    }
}

/**
 * 设备动态信息卡片
 *
 * 显示单个相机设备的状态信息，包括分辨率、帧率、活跃流等
 *
 * @param deviceInfo 相机设备信息
 */
@Composable
private fun DeviceInfoCard(deviceInfo: CameraDeviceInfo) {
    val isActive = deviceInfo.isOpen
    val backgroundColor = if (isActive) Color(0xFFE3F2FD) else Color.White

    InfoCard(
        title = "Camera Device ${deviceInfo.deviceId}",
        icon = Icons.Default.Videocam,
        backgroundColor = backgroundColor
    ) {
        // 设备状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = null,
                tint = if (isActive) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "活跃" else "已关闭",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFF4CAF50) else Color.Gray
            )

            if (deviceInfo.cameraState.isNotEmpty()) {
                Spacer(modifier = Modifier.width(16.dp))
                StatusBadge(text = deviceInfo.cameraState)
            }
        }

        if (!isActive) return@InfoCard

        Spacer(modifier = Modifier.height(12.dp))

        // 客户端信息
        deviceInfo.clientInfo?.let { client ->
            InfoRow(label = "应用包名", value = client.packageName, valueColor = Color(0xFF1976D2))
            InfoRow(label = "进程ID (PID)", value = client.pid.toString())
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 分辨率配置
        Text(
            text = "📐 分辨率配置",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResolutionItem(
                label = "预览",
                resolution = deviceInfo.previewConfig.resolution,
                fps = deviceInfo.previewConfig.fpsRange
            )
            ResolutionItem(
                label = "拍照",
                resolution = deviceInfo.captureConfig.resolution,
                fps = ""
            )
            ResolutionItem(
                label = "视频",
                resolution = deviceInfo.videoConfig.resolution,
                fps = ""
            )
        }

        // 流信息
        if (deviceInfo.streamList.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "📊 活跃流 (${deviceInfo.streamList.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(8.dp))

            deviceInfo.streamList.forEach { stream ->
                StreamItem(stream)
            }
        }
    }
}

/**
 * 分辨率项
 *
 * @param label 标签 (预览/拍照/视频)
 * @param resolution 分辨率字符串
 * @param fps 帧率字符串
 */
@Composable
private fun ResolutionItem(
    label: String,
    resolution: String,
    fps: String
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
            text = resolution,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        if (fps.isNotEmpty()) {
            Text(
                text = fps,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

/**
 * 流信息项（丰富版本）
 *
 * 显示流的详细信息，包括格式、分辨率、帧率等
 *
 * @param stream 相机流信息
 */
@Composable
private fun StreamItem(stream: CameraStream) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 第一行：流ID和类型 + 分辨率
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Stream[${stream.streamId}]",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (stream.type == "Output") Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color(0xFF2196F3).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stream.type,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (stream.type == "Output") Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
                }
            }

            // 分辨率
            Text(
                text = stream.resolution,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第二行：消费者名称
        Text(
            text = stream.consumerName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 第三行：格式信息
        Column {
            Text(
                text = "格式",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = stream.formatName.ifEmpty { stream.format },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        // 第四行：数据空间（如果有，单独一行显示完整信息）
        if (stream.dataSpace.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "DataSpace",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = DataSpaceHelper.getFormattedDataSpace(stream.dataSpace),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 第五行：用途（如果有，单独一行显示完整信息）
        if (stream.usage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "Usage",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = UsageHelper.getFormattedUsage(stream.usage),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第六行：帧统计和实时帧率
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 总帧数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "帧数: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = stream.framesProduced.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 实时帧率
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = getFpsColor(stream.calculatedFps).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "FPS: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = String.format("%.1f", stream.calculatedFps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getFpsColor(stream.calculatedFps)
                )
            }
        }
    }
}

/**
 * 根据帧率获取颜色
 *
 * @param fps 帧率
 * @return 对应的颜色
 */
private fun getFpsColor(fps: Float): Color {
    return when {
        fps >= 25f -> Color(0xFF4CAF50) // 绿色 - 流畅
        fps >= 15f -> Color(0xFFFF9800) // 橙色 - 一般
        fps > 0f -> Color(0xFFF44336)   // 红色 - 低帧率
        else -> Color.Gray               // 灰色 - 无数据
    }
}

/**
 * 事件日志卡片
 *
 * @param logs 事件日志列表
 */
@Composable
private fun EventLogsCard(logs: List<CameraEventLog>) {
    InfoCard(title = "📋 最近事件日志 (${logs.size})") {
        logs.forEach { log ->
            EventLogItem(log)
        }
    }
}

/**
 * 事件日志项
 *
 * @param log 事件日志
 */
@Composable
private fun EventLogItem(log: CameraEventLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 事件类型颜色指示
        val (color, icon) = when (log.eventType) {
            CameraEventType.CONNECT -> Pair(Color(0xFF4CAF50), "▶")
            CameraEventType.DISCONNECT -> Pair(Color(0xFFFF9800), "⏹")
            CameraEventType.DIED -> Pair(Color(0xFFF44336), "💀")
            CameraEventType.UNKNOWN -> Pair(Color.Gray, "?")
        }

        Text(
            text = icon,
            color = color
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.eventType.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            if (log.packageName.isNotEmpty()) {
                Text(
                    text = "${log.packageName} (PID: ${log.pid}) - Device ${log.deviceId}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (log.reason.isNotEmpty()) {
                Text(
                    text = "PID: ${log.pid} - ${log.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

// ==================== 通用组件 ====================

/**
 * 信息卡片
 *
 * @param title 卡片标题
 * @param icon 可选的图标
 * @param backgroundColor 背景色
 * @param content 卡片内容
 */
@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector? = null,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 信息行
 *
 * @param label 标签
 * @param value 值
 * @param valueColor 值的颜色
 * @param modifier Modifier
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
 * 状态标签
 *
 * @param text 标签文本
 */
@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF1976D2).copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
    }
}
