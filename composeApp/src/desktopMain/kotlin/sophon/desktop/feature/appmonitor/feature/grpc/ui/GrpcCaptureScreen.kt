package sophon.desktop.feature.appmonitor.feature.grpc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.appmonitor.feature.grpc.model.GrpcCaptureModel
import sophon.desktop.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 状态颜色常量 — 根据 gRPC statusCode 赋予不同颜色
// ============================================================================
/** gRPC OK (0) */
private val STATUS_OK_COLOR = Color(0xFF16A34A)

/** gRPC 错误 (非0) */
private val STATUS_ERROR_COLOR = Color(0xFFDC2626)

/** gRPC 未知 (-1 / 默认) */
private val STATUS_UNKNOWN_COLOR = Color(0xFF9CA3AF)

// ============================================================================
// 主入口
// ============================================================================

/**
 * gRPC 流量抓取页面（类 Charles 双栏布局）
 *
 * 左栏：请求列表概览（service_url、状态、时间）
 * 右栏：点击某条请求后展示完整请求/响应详情
 *
 * @param packageName 当前前台应用包名，为 null 时显示提示信息
 * @param modifier    Compose 修饰符
 * @param vm          gRPC 捕获 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrpcCaptureScreen(
    packageName: String?,
    modifier: Modifier = Modifier,
    vm: GrpcCaptureViewModel = viewModel { GrpcCaptureViewModel() }
) {
    val uiState by vm.uiState.collectAsState()
    // 当前选中的请求索引
    var selectedIndex by remember { mutableStateOf(-1) }

    // 当 packageName 变化时自动启动轮询
    LaunchedEffect(packageName) {
        if (!packageName.isNullOrBlank()) {
            vm.startPolling(packageName)
        }
    }

    // 页面离开时停止轮询
    DisposableEffect(Unit) {
        onDispose {
            vm.stopPolling()
        }
    }

    Scaffold{ padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is GrpcCaptureUiState.Idle -> {
                    CenterMessage(
                        icon = Icons.Default.BugReport,
                        message = if (!packageName.isNullOrBlank()) ""
                        else "等待获取前台应用包名..."
                    )
                }

                is GrpcCaptureUiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                }

                is GrpcCaptureUiState.Success -> {
                    if (state.records.isEmpty()) {
                        CenterMessage(
                            icon = Icons.Default.BugReport,
                            message = "数据库连接成功，但表中暂无记录"
                        )
                    } else {
                        // 防止越界
                        val selected = if (selectedIndex in state.records.indices)
                            state.records[selectedIndex] else null

                        // ── 双栏布局 ─────────────────────────────────
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 左栏：请求列表
                            RequestListPanel(
                                records = state.records,
                                selectedIndex = selectedIndex,
                                onSelect = { selectedIndex = it },
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                            )

                            // 分隔线
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            // 右栏：请求详情
                            RequestDetailPanel(
                                record = selected,
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                is GrpcCaptureUiState.Error -> {
                    // 错误页面：展示错误信息和重试按钮，轮询已自动停止
                    ErrorPage(
                        message = state.message,
                        onRetry = {
                            selectedIndex = -1
                            vm.refreshData(packageName)
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// 左栏：请求列表面板
// ============================================================================

/**
 * 请求列表面板（类似 Charles 左侧列表）
 *
 * 每行展示：状态指示圆点 + serviceUrl（末段方法名加粗）+ 时间
 *
 * @param records       所有捕获的 gRPC 记录列表
 * @param selectedIndex 当前选中的记录索引
 * @param onSelect      点击条目回调
 * @param modifier      外部修饰符
 */
@Composable
private fun RequestListPanel(
    records: List<GrpcCaptureModel>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 请求列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(records) { index, record ->
                RequestListItem(
                    record = record,
                    isSelected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

/**
 * 单条请求列表项
 *
 * @param record     gRPC 捕获记录
 * @param isSelected 是否为当前选中项
 * @param onClick    点击回调
 */
@Composable
private fun RequestListItem(
    record: GrpcCaptureModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when {
        record.statusCode == 0 -> STATUS_OK_COLOR
        record.statusCode > 0 -> STATUS_ERROR_COLOR
        else -> STATUS_UNKNOWN_COLOR
    }

    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = Dimens.paddingMedium, vertical = Dimens.paddingSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 状态指示圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = record.serviceName.ifEmpty { record.serviceUrl.ifEmpty { "-" } },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatTimestamp(record.createTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 底部分隔线
        HorizontalDivider(
            modifier = Modifier.padding(top = Dimens.paddingSmall),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }
}

// ============================================================================
// 右栏：请求详情面板
// ============================================================================

/**
 * 请求详情面板（类似 Charles 右侧详情页）
 *
 * 分段展示 Overview、Request Headers、Request Body、Response Headers、
 * Response Body、Status 信息
 *
 * @param record 当前选中的 gRPC 捕获记录，为 null 时显示空白占位
 * @param modifier 外部修饰符
 */
@Composable
private fun RequestDetailPanel(
    record: GrpcCaptureModel?,
    modifier: Modifier = Modifier
) {
    if (record == null) {
        // 未选中任何请求 — 空白占位
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            Text(
                text = "← 选择一条请求查看详情",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.paddingLarge)
    ) {
        // ── 概览 ──────────────────────────────────────────────────────────
        DetailSectionTitle("概览")
        DetailRow("Service URL", record.serviceUrl)
        DetailRow("Service Name", record.serviceName)
        DetailRow("Status", buildStatusSummary(record))
        DetailRow("请求时间", formatTimestampFull(record.createTimestamp))
        DetailRow("更新时间", formatTimestampFull(record.updateTimestamp))

        Spacer(modifier = Modifier.height(Dimens.spacerMedium))

        // ── 状态信息 ──────────────────────────────────────────────────────
        if (record.statusCode != 0 || record.statusDesc.isNotBlank() || record.statusErrorCause.isNotBlank()) {
            DetailSectionTitle("状态")
            DetailRow("Code", "${record.statusCode}")
            DetailRow("Level", "${record.statusLevel}")
            DetailRow("Name", record.statusName)
            if (record.statusDesc.isNotBlank()) {
                DetailRow("Description", record.statusDesc)
            }
            if (record.statusErrorCause.isNotBlank()) {
                DetailCodeBlock("Error Cause", record.statusErrorCause)
            }
            Spacer(modifier = Modifier.height(Dimens.spacerMedium))
        }

        // ── Request Headers ──────────────────────────────────────────────
        DetailSectionTitle("Request Headers")
        if (record.requestHeader.isNotBlank()) {
            HeaderTable(record.requestHeader)
        } else {
            DetailEmpty("无请求头")
        }

        Spacer(modifier = Modifier.height(Dimens.spacerMedium))

        // ── Request Body ─────────────────────────────────────────────────
        DetailSectionTitle("Request Body")
        if (record.requestBody.isNotBlank()) {
            DetailCodeBlock(content = record.requestBody)
        } else {
            DetailEmpty("无请求体")
        }

        Spacer(modifier = Modifier.height(Dimens.spacerMedium))

        // ── Response Headers ─────────────────────────────────────────────
        DetailSectionTitle("Response Headers")
        if (record.responseHeader.isNotBlank()) {
            HeaderTable(record.responseHeader)
        } else {
            DetailEmpty("无响应头")
        }

        Spacer(modifier = Modifier.height(Dimens.spacerMedium))

        // ── Response Body ────────────────────────────────────────────────
        DetailSectionTitle("Response Body")
        if (record.responseBody.isNotBlank()) {
            DetailCodeBlock(content = record.responseBody)
        } else {
            DetailEmpty("无响应体")
        }

        Spacer(modifier = Modifier.height(Dimens.spacerLarge))
    }
}

// ============================================================================
// 详情辅助组件
// ============================================================================

/**
 * 详情区域标题（带底部边框）
 *
 * @param title 标题文本
 */
@Composable
private fun DetailSectionTitle(title: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Dimens.paddingSmall)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = Dimens.paddingMedium)
        )
    }
}

/**
 * 键值对行
 *
 * @param label 标签文本
 * @param value 值文本
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 代码块/长文本区域
 *
 * @param title   可选标题（如 "Error Cause"），为 null 则不显示标题
 * @param content 代码块内容
 */
@Composable
private fun DetailCodeBlock(title: String? = null, content: String) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimens.paddingSmall)
            )
        }
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimens.cornerRadiusSmall)
                )
                .padding(Dimens.paddingMedium)
        )
    }
}

/**
 * 空内容占位提示
 *
 * @param message 提示文本
 */
@Composable
private fun DetailEmpty(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = Dimens.paddingSmall)
    )
}

// ============================================================================
// 通用提示组件
// ============================================================================

/**
 * 居中图标 + 文字提示（用于空状态、错误状态）
 *
 * @param icon    显示的图标
 * @param message 提示文本
 * @param isError 是否为错误状态
 */
@Composable
private fun CenterMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    isError: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 错误页面（带重试按钮）
 *
 * 当数据库拉取失败时展示此页面，轮询已自动停止。
 * 用户可通过重试按钮重新启动轮询。
 *
 * @param message 错误描述信息
 * @param onRetry 重试按钮回调
 */
@Composable
private fun ErrorPage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("重试")
        }
    }
}

// ============================================================================
// Headers 表格组件
// ============================================================================

/**
 * 将 headers 字符串解析为键值对并以表格方式展示
 *
 * 支持多种常见 header 格式的解析：
 * - "key: value" 或 "key=value"（每行一对）
 * - JSON 格式（如 {"key":"value"}）
 * - 逗号分隔形式
 *
 * @param rawHeaders 原始 headers 字符串
 */
@Composable
private fun HeaderTable(rawHeaders: String) {
    val headerPairs = parseHeaders(rawHeaders)

    if (headerPairs.isEmpty()) {
        // 无法解析时 fallback 为代码块原始展示
        DetailCodeBlock(content = rawHeaders)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(Dimens.cornerRadiusSmall)
            )
    ) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(
                        topStart = Dimens.cornerRadiusSmall,
                        topEnd = Dimens.cornerRadiusSmall
                    )
                )
                .padding(horizontal = Dimens.paddingMedium, vertical = Dimens.paddingSmall)
        ) {
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = "Value",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.6f)
            )
        }

        // 数据行
        headerPairs.forEachIndexed { index, (key, value) ->
            val rowBg = if (index % 2 == 0) Color.Transparent
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .padding(horizontal = Dimens.paddingMedium, vertical = 3.dp)
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.6f)
                )
            }
        }
    }
}

// ============================================================================
// 工具函数
// ============================================================================

/** 时间格式器：仅时:分:秒（列表项用） */
private val timeOnlyFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** 时间格式器：年-月-日 时:分:秒.毫秒（详情用） */
private val fullDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

/**
 * 格式化时间戳为 HH:mm:ss
 *
 * @param timestamp 毫秒级时间戳
 * @return 格式化后的时间字符串，时间戳为 0 时返回 "-"
 */
private fun formatTimestamp(timestamp: Long): String {
    return if (timestamp > 0) timeOnlyFormatter.format(Date(timestamp)) else "-"
}

/**
 * 格式化时间戳为完整日期时间
 *
 * @param timestamp 毫秒级时间戳
 * @return 格式化后的日期时间字符串，时间戳为 0 时返回 "-"
 */
private fun formatTimestampFull(timestamp: Long): String {
    return if (timestamp > 0) fullDateFormatter.format(Date(timestamp)) else "-"
}

/**
 * 构建状态摘要文本
 *
 * @param record gRPC 捕获记录
 * @return 状态摘要字符串，例如 "OK (0)" 或 "UNAVAILABLE (14)"
 */
private fun buildStatusSummary(record: GrpcCaptureModel): String {
    val name = record.statusName.ifBlank { "UNKNOWN" }
    return "$name (${record.statusCode})"
}

/**
 * 解析 Protodroid Metadata headers 字符串为键值对列表
 *
 * 实际格式为 gRPC Metadata.toString() 的输出：
 * `Metadata(uid=4735899,region=XM,access-control-allow-methods=GET,POST,PUT,...)`
 *
 * 解析难点：value 本身可能包含逗号（如 `GET,POST,PUT,DELETE`），
 * 因此不能简单按逗号分割。采用正则在「,headerName=」边界处分割：
 * header name 的合法字符为字母、数字、下划线、连字符。
 *
 * @param raw 原始 headers 字符串（Metadata(...) 格式）
 * @return 解析后的键值对列表，解析失败时返回空列表
 */
private fun parseHeaders(raw: String): List<Pair<String, String>> {
    var content = raw.trim()

    // 去除 Metadata(...) 包裹
    if (content.startsWith("Metadata(") && content.endsWith(")")) {
        content = content.removePrefix("Metadata(").removeSuffix(")")
    }

    if (content.isBlank()) return emptyList()

    // 使用正则在「,headerName=」边界处分割
    // 正向前瞻：逗号后面紧跟 [合法header字符]+=
    // 这样 value 中的逗号（如 GET,POST）不会被误切
    val segments = content.split(Regex(",(?=[a-zA-Z][a-zA-Z0-9_-]*=)"))

    val pairs = mutableListOf<Pair<String, String>>()
    for (segment in segments) {
        val eqIndex = segment.indexOf('=')
        if (eqIndex > 0) {
            val key = segment.substring(0, eqIndex).trim()
            val value = segment.substring(eqIndex + 1).trim()
            pairs.add(key to value)
        }
    }
    return pairs
}
