package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import sophon.desktop.feature.packetcapture.data.source.grpc.GrpcBodyDecoder
import sophon.desktop.feature.packetcapture.model.CapturedPacket

private val prettyJson = Json { prettyPrint = true }

private fun formatJsonOrRaw(text: String?): String {
    if (text.isNullOrBlank()) return "(空)"
    return try {
        val element = Json.parseToJsonElement(text)
        prettyJson.encodeToString(JsonElement.serializer(), element)
    } catch (_: Exception) {
        text
    }
}

private fun isJson(contentType: String?): Boolean =
    contentType?.contains("json", ignoreCase = true) == true

private val methodDetailColors = mapOf(
    "GET" to Color(0xFF2196F3),
    "POST" to Color(0xFF4CAF50),
    "PUT" to Color(0xFFFF9800),
    "DELETE" to Color(0xFFF44336),
    "PATCH" to Color(0xFF9C27B0),
    "HEAD" to Color(0xFF00BCD4),
    "OPTIONS" to Color(0xFF607D8B),
)

private fun detailStatusColor(statusCode: Int?): Color = when {
    statusCode == null -> Color(0xFF9E9E9E)
    statusCode < 300 -> Color(0xFF4CAF50)
    statusCode < 400 -> Color(0xFFFF9800)
    statusCode < 500 -> Color(0xFFF44336)
    else -> Color(0xFF9C27B0)
}

// JSON 树视图中各节点共用的文字样式（在 Composable 上下文中读取 MaterialTheme）
private val jsonMonoStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )

/**
 * Charles Proxy 风格的请求详情面板。
 *
 * 顶部：URL 摘要行（method → url → status）。
 * 主标签页：概览 / 内容。
 * 内容页内部垂直分割：请求区（上，weight=0.4）+ 响应格式选择标签（中）+ 响应区（下，weight=0.6）。
 */
@Composable
fun PacketDetailPanel(
    packet: CapturedPacket,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(packet.id) { mutableIntStateOf(1) }

    Column(modifier = modifier) {
        RequestSummaryBar(packet)
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            listOf("概览", "内容").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> OverviewTab(packet)
                1 -> ContentsTab(packet)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// URL 摘要行
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RequestSummaryBar(packet: CapturedPacket) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val methodLabel = if (packet is CapturedPacket.Grpc) "gRPC" else packet.method
            val methodColor = if (packet is CapturedPacket.Grpc) Color(0xFF7B1FA2)
            else methodDetailColors[packet.method] ?: Color(0xFF607D8B)

            Text(
                text = methodLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = methodColor,
                modifier = Modifier.width(40.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = packet.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = packet.statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = detailStatusColor(packet.statusCode)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 概览 Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(packet: CapturedPacket) {
    SelectionContainer {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (packet is CapturedPacket.Grpc) {
                OverviewRow("类型", "gRPC", valueColor = Color(0xFF7B1FA2))
                OverviewRow("服务", packet.service ?: "-")
                OverviewRow("RPC 方法", packet.rpcMethod ?: "-")
                OverviewRow("URL", packet.url)
                OverviewRow("协议", "HTTP/2 (gRPC)")
            } else {
                OverviewRow("方法", packet.method)
                OverviewRow("URL", packet.url)
                OverviewRow("协议", packet.scheme.uppercase())
            }
            OverviewRow("状态码", packet.statusCode?.toString() ?: "-")
            OverviewRow("耗时", packet.durationMs?.let { "${it}ms" } ?: "-")
            OverviewRow("请求大小", "${packet.requestBodySize()} bytes")
            OverviewRow("响应大小", "${packet.responseBodySize()} bytes")
            if (packet.error != null) {
                OverviewRow("错误", packet.error!!, valueColor = Color(0xFFF44336))
            }
        }
    }
}

@Composable
private fun OverviewRow(label: String, value: String, valueColor: Color = Color(0xFF1A1A1A)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Color(0xFF666666),
            modifier = Modifier.weight(0.25f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = valueColor,
            modifier = Modifier.weight(0.75f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 内容 Tab（请求区上 + 响应区下）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContentsTab(packet: CapturedPacket) {
    val isGrpc = packet is CapturedPacket.Grpc

    // HTTP 响应子 Tab：响应头 / 文本 / JSON / JSON Text
    // gRPC 响应子 Tab：响应头 / Proto / 文本
    val responseTabs = if (isGrpc) {
        listOf("响应头", "Proto", "文本")
    } else {
        listOf("响应头", "文本", "JSON", "JSON Text")
    }
    val defaultResponseTab = if (isGrpc) 1 else {
        val ct = packet.responseHeaders["Content-Type"] ?: packet.responseHeaders["content-type"]
        if (isJson(ct)) 2 else 1
    }
    var responseTab by remember(packet.id) { mutableIntStateOf(defaultResponseTab) }

    // gRPC Proto 解析（懒加载，remember 缓存避免重复计算）
    val reqDecoded = if (isGrpc && packet.requestBody != null) {
        remember(packet.id) {
            GrpcBodyDecoder.decode(packet.requestBody!!, packet.path, isRequest = true)
        }
    } else null
    val respDecoded = if (isGrpc && packet.responseBody != null) {
        remember(packet.id) {
            GrpcBodyDecoder.decode(packet.responseBody!!, packet.path, isRequest = false)
        }
    } else null

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {

        // ── 请求区（上，weight=0.4）──────────────────────────────────────────
        Column(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
            if (isGrpc) {
                // gRPC：标签栏 + Schema 状态标注
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "请求 Proto",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (reqDecoded != null) {
                            val schemaLabel = if (reqDecoded.isSchemaApplied) "Schema" else "无 Schema"
                            val schemaColor = if (reqDecoded.isSchemaApplied) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            Text(
                                text = schemaLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = schemaColor
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                if (reqDecoded != null) {
                    BodySection(reqDecoded.body, formatAsJson = reqDecoded.isSchemaApplied)
                } else {
                    BodySection(null, formatAsJson = false)
                }
            } else {
                // HTTP：请求头 / 请求体 子 Tab
                var requestTab by remember(packet.id) { mutableIntStateOf(0) }
                ScrollableTabRow(
                    selectedTabIndex = requestTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    divider = {}
                ) {
                    listOf("请求头", "请求体").forEachIndexed { index, title ->
                        Tab(
                            selected = requestTab == index,
                            onClick = { requestTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                when (requestTab) {
                    0 -> RequestHeadersSection(packet.requestHeaders)
                    else -> RequestBodySection(packet)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── 响应子 Tab 标签行 ─────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = responseTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            divider = {}
        ) {
            responseTabs.forEachIndexed { index, title ->
                Tab(
                    selected = responseTab == index,
                    onClick = { responseTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── 响应内容区（下，weight=0.6）────────────────────────────────────────
        Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            if (isGrpc) {
                when (responseTab) {
                    0 -> ResponseHeadersSection(packet.responseHeaders)
                    1 -> BodySection(respDecoded?.body, formatAsJson = respDecoded?.isSchemaApplied == true)
                    2 -> BodySection(packet.responseBodyAsText(), formatAsJson = false)
                    else -> BodySection(null, formatAsJson = false)
                }
            } else {
                when (responseTab) {
                    0 -> ResponseHeadersSection(packet.responseHeaders)
                    1 -> BodySection(packet.responseBodyAsText(), formatAsJson = false)       // 文本：原始字符串
                    2 -> JsonTreeView(packet.responseBodyAsText())                            // JSON：折叠树视图
                    3 -> BodySection(packet.responseBodyAsText(), formatAsJson = true)        // JSON Text：格式化展示，不可折叠
                    else -> BodySection(null, formatAsJson = false)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Headers / Body sections
// ─────────────────────────────────────────────────────────────────────────────

/** 请求头列表，与响应头保持相同的视觉风格（原始顺序，不排序）。 */
@Composable
private fun RequestHeadersSection(headers: Map<String, String>) {
    if (headers.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
            contentAlignment = Alignment.Center
        ) {
            Text("(无请求头)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    SelectionContainer {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            headers.entries.forEach { (key, value) ->
                HeaderRow(key = key, value = value)
            }
        }
    }
}

/**
 * 请求体展示区域，与请求头共用相同的容器样式。
 * - 无内容：显示 "(空)" 占位
 * - JSON body：折叠树视图
 * - 其他：等宽纯文本
 */
@Composable
private fun RequestBodySection(packet: CapturedPacket) {
    val reqBody = packet.requestBodyAsText()
    if (reqBody.isNullOrEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
            contentAlignment = Alignment.Center
        ) {
            Text("(空)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val reqContentType = packet.requestHeaders["content-type"]
        ?: packet.requestHeaders["Content-Type"]
    if (isJson(reqContentType)) {
        JsonTreeView(reqBody)
    } else {
        BodySection(reqBody, formatAsJson = false)
    }
}

@Composable
private fun ResponseHeadersSection(headers: Map<String, String>) {
    if (headers.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
            contentAlignment = Alignment.Center
        ) {
            Text("(无响应头)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    SelectionContainer {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            headers.entries.sortedBy { it.key }.forEach { (key, value) ->
                HeaderRow(key = key, value = value)
            }
        }
    }
}

@Composable
private fun HeaderRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = Color(0xFF0277BD),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.weight(0.65f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Body section（纯文本 / 格式化文本）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BodySection(bodyText: String?, formatAsJson: Boolean) {
    val text = when {
        bodyText.isNullOrEmpty() -> "(空)"
        formatAsJson -> formatJsonOrRaw(bodyText)
        else -> bodyText
    }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                color = Color(0xFF212121)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JSON 折叠树视图
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JsonTreeView(bodyText: String?) {
    if (bodyText.isNullOrEmpty()) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
            Text("(空)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val rootElement = remember(bodyText) {
        runCatching { Json.parseToJsonElement(bodyText) }.getOrNull()
    }
    // JSON 解析失败时降级为纯文本
    if (rootElement == null) {
        BodySection(bodyText, formatAsJson = false)
        return
    }
    SelectionContainer {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            JsonNodeView(key = null, element = rootElement, depth = 0)
        }
    }
}

@Composable
private fun JsonNodeView(key: String?, element: JsonElement, depth: Int) {
    when (element) {
        is JsonObject -> JsonObjectView(key = key, obj = element, depth = depth)
        is JsonArray -> JsonArrayView(key = key, arr = element, depth = depth)
        is JsonPrimitive -> JsonPrimitiveRow(key = key, primitive = element, depth = depth)
    }
}

@Composable
private fun JsonObjectView(key: String?, obj: JsonObject, depth: Int) {
    var expanded by remember { mutableStateOf(true) }
    val startPadding = (depth * 16).dp

    // 标题行：可点击切换展开/收起
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "▼ " else "▶ ",
            style = jsonMonoStyle,
            color = Color(0xFF9E9E9E)
        )
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        if (expanded) {
            Text("{", style = jsonMonoStyle, color = Color(0xFF616161))
        } else {
            Text("{ ${obj.size} }", style = jsonMonoStyle, color = Color(0xFF9E9E9E))
        }
    }

    if (expanded) {
        obj.entries.forEach { (k, v) ->
            JsonNodeView(key = k, element = v, depth = depth + 1)
        }
        Row(modifier = Modifier.padding(start = startPadding + 16.dp, top = 1.dp, bottom = 1.dp)) {
            Text("}", style = jsonMonoStyle, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun JsonArrayView(key: String?, arr: JsonArray, depth: Int) {
    var expanded by remember { mutableStateOf(true) }
    val startPadding = (depth * 16).dp

    // 标题行：可点击切换展开/收起
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "▼ " else "▶ ",
            style = jsonMonoStyle,
            color = Color(0xFF9E9E9E)
        )
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        if (expanded) {
            Text("[", style = jsonMonoStyle, color = Color(0xFF616161))
        } else {
            Text("[ ${arr.size} ]", style = jsonMonoStyle, color = Color(0xFF9E9E9E))
        }
    }

    if (expanded) {
        arr.forEach { element ->
            JsonNodeView(key = null, element = element, depth = depth + 1)
        }
        Row(modifier = Modifier.padding(start = startPadding + 16.dp, top = 1.dp, bottom = 1.dp)) {
            Text("]", style = jsonMonoStyle, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun JsonPrimitiveRow(key: String?, primitive: JsonPrimitive, depth: Int) {
    // +16.dp 对齐到已展开子节点的起始位置
    val startPadding = (depth * 16).dp + 16.dp

    val (valueText, valueColor) = when {
        primitive is JsonNull -> "null" to Color(0xFF9E9E9E)
        primitive.isString -> "\"${primitive.content}\"" to Color(0xFF2E7D32)
        primitive.content == "true" -> "true" to Color(0xFF1565C0)
        primitive.content == "false" -> "false" to Color(0xFFC62828)
        else -> primitive.content to Color(0xFFBF360C)  // 数字
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        Text(valueText, style = jsonMonoStyle, color = valueColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 空状态
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyDetailPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
        Text(
            "选择一条请求查看详情",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
