package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import sophon.desktop.feature.packetcapture.model.DecodedBody
import sophon.desktop.feature.packetcapture.model.FileDownloadInfo
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private fun formatJsonOrRaw(text: String?): String {
    if (text.isNullOrBlank()) return "(空)"
    return text
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
    decodedBody: DecodedBody?,
    isDecodingBody: Boolean,
    onSaveFile: () -> Unit,
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
                1 -> ContentsTab(
                    packet = packet,
                    decoded = decodedBody,
                    isDecoding = isDecodingBody,
                    onSaveFile = onSaveFile,
                )
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
private fun ContentsTab(
    packet: CapturedPacket,
    decoded: DecodedBody?,
    isDecoding: Boolean,
    onSaveFile: () -> Unit,
) {
    val isGrpc = packet is CapturedPacket.Grpc

    val responseTabs = if (isGrpc) listOf("响应头", "Proto", "文本")
    else listOf("响应头", "文本", "JSON", "JSON Text")

    val defaultResponseTab = if (isGrpc) 1 else {
        val ct = packet.responseHeaders["Content-Type"] ?: packet.responseHeaders["content-type"]
        if (isJson(ct)) 2 else 1
    }
    var responseTab by remember(packet.id) { mutableIntStateOf(defaultResponseTab) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {

        // ── 请求区（上，weight=0.4）──────────────────────────────────────────
        Column(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
            if (isGrpc) {
                val grpcReqDecoded = decoded?.grpcRequest
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
                        if (grpcReqDecoded != null) {
                            val schemaLabel = if (grpcReqDecoded.isSchemaApplied) "Schema" else "无 Schema"
                            val schemaColor = if (grpcReqDecoded.isSchemaApplied) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            Text(
                                text = schemaLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = schemaColor
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                // 使用后台预计算的 formattedBody（schema 已应用时）或原始 body
                BodySection(grpcReqDecoded?.formattedBody ?: grpcReqDecoded?.body, formatAsJson = false)
            } else {
                val queryParams = remember(packet.id) { parseQueryParams(packet.path) }
                val requestTabs = remember(packet.id) {
                    buildList {
                        add("请求头")
                        if (queryParams.isNotEmpty()) add("Query 参数")
                        add("请求体")
                    }
                }
                var requestTab by remember(packet.id) { mutableIntStateOf(0) }
                ScrollableTabRow(
                    selectedTabIndex = requestTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    divider = {}
                ) {
                    requestTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = requestTab == index,
                            onClick = { requestTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                when (requestTabs.getOrNull(requestTab)) {
                    "请求头" -> RequestHeadersSection(packet.requestHeaders)
                    "Query 参数" -> QueryParamsSection(queryParams)
                    // 传入后台解码的请求体文本，避免在主线程解压
                    else -> RequestBodySection(packet, decoded?.requestText, decoded?.requestJson)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── 响应区（下，weight=0.6）──────────────────────────────────────────
        Column(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            when {
                isDecoding -> LoadingSection(modifier = Modifier.fillMaxSize())
                decoded?.isFileDownload == true -> FileDownloadSection(
                    info = decoded.fileInfo!!,
                    bodyAvailable = decoded.bodyAvailable,
                    onSave = onSaveFile,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> {
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
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isGrpc) {
                            val grpcRespDecoded = decoded?.grpcResponse
                            when (responseTab) {
                                0 -> ResponseHeadersSection(packet.responseHeaders)
                                // 使用后台预计算的 formattedBody（schema 已应用时）或原始 body
                                1 -> BodySection(grpcRespDecoded?.formattedBody ?: grpcRespDecoded?.body, formatAsJson = false)
                                2 -> BodySection(decoded?.responseText, formatAsJson = false)
                                else -> BodySection(null, formatAsJson = false)
                            }
                        } else {
                            val respText = decoded?.responseText
                            val respJson = decoded?.responseJson
                            val isLargeBody = (respText?.length ?: 0) > LARGE_BODY_LIMIT
                            when (responseTab) {
                                0 -> ResponseHeadersSection(packet.responseHeaders)
                                1 -> BodySection(respText, formatAsJson = false)
                                2 -> if (isLargeBody) {
                                    LargeBodyFallback(respText, "响应体超出 ${LARGE_BODY_LIMIT / 1000}KB，JSON 树视图已跳过")
                                } else {
                                    JsonTreeView(respJson)
                                }
                                // JSON Text：直接用后台预计算的 pretty-print 文本，无需主线程 JSON 解析
                                3 -> BodySection(decoded?.responsePrettyJson ?: respText, formatAsJson = false)
                                else -> BodySection(null, formatAsJson = false)
                            }
                        }
                    }
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
 * 请求体展示区域。
 * - [requestText]：后台线程预解码的请求体文本（已解压），避免在主线程做 gzip/deflate 解压。
 * - [requestJson]：后台预解析的 [JsonElement]，content-type 为 JSON 时展示折叠树。
 */
@Composable
private fun RequestBodySection(
    packet: CapturedPacket,
    requestText: String?,
    requestJson: JsonElement?,
) {
    if (requestText.isNullOrEmpty()) {
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
    if (isJson(reqContentType) && requestJson != null) {
        JsonTreeView(requestJson)
    } else {
        BodySection(requestText, formatAsJson = false)
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
// 加载中 / 文件下载 / 超大 body 降级 组件
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFFFAFAFA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(8.dp))
            Text("解码中…", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun FileDownloadSection(
    info: FileDownloadInfo,
    bodyAvailable: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    SelectionContainer {
        Column(
            modifier = modifier
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "文件下载",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            FileInfoRow("文件名", info.fileName)
            FileInfoRow("类型", info.contentType)
            FileInfoRow(
                "大小",
                if (info.sizeBytes >= 0) "${info.formattedSize()}（${info.sizeBytes} 字节）" else "未知",
            )
            if (info.md5 != null) FileInfoRow("MD5", info.md5)
            if (info.etag != null) FileInfoRow("ETag", info.etag)
            if (info.lastModified != null) FileInfoRow("最后修改", info.lastModified)
            Spacer(Modifier.height(20.dp))
            if (bodyAvailable) {
                Button(onClick = onSave) {
                    Text("保存响应体到文件…")
                }
            } else {
                Text(
                    text = "响应体未完整捕获（文件已完整转发到设备端）",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                )
            }
        }
    }
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = Color(0xFF666666),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LargeBodyFallback(text: String?, hint: String) {
    val scrollState = rememberScrollState()
    val hScroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFF9800),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        SelectionContainer {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .horizontalScroll(hScroll)
            ) {
                Text(
                    text = text ?: "(空)",
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
}

/** body 超过此字符数时跳过 JSON 树渲染，降级为纯文本。 */
private const val LARGE_BODY_LIMIT = 200_000

// ─────────────────────────────────────────────────────────────────────────────
// JSON 折叠树视图
// ─────────────────────────────────────────────────────────────────────────────

/**
 * JSON 折叠树视图。
 * [element] 由后台线程预解析传入，此函数不再在主线程做 JSON 解析。
 */
@Composable
private fun JsonTreeView(element: JsonElement?) {
    if (element == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
            Text("(空)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
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
                .padding(12.dp)
        ) {
            JsonNodeView(key = null, element = element, depth = 0)
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
    // 根节点（depth=0）默认展开；嵌套节点默认收起，减少初始渲染压力
    var expanded by remember { mutableStateOf(depth == 0) }
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
    // 根节点（depth=0）默认展开；嵌套节点默认收起，减少初始渲染压力
    var expanded by remember { mutableStateOf(depth == 0) }
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
// Query 参数解析与展示
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 从 path（含 query string）中解析出参数键值对。
 * 支持 URL 编码的 key/value，保留同名参数的多值（List<Pair>）。
 */
private fun parseQueryParams(path: String): List<Pair<String, String>> {
    val queryIndex = path.indexOf('?')
    if (queryIndex < 0) return emptyList()
    val queryString = path.substring(queryIndex + 1)
    if (queryString.isBlank()) return emptyList()
    return queryString.split('&').mapNotNull { pair ->
        val eqIdx = pair.indexOf('=')
        if (eqIdx < 0) {
            val key = runCatching { URLDecoder.decode(pair, StandardCharsets.UTF_8.name()) }.getOrDefault(pair)
            if (key.isNotEmpty()) key to "" else null
        } else {
            val rawKey = pair.substring(0, eqIdx)
            val rawVal = pair.substring(eqIdx + 1)
            val key = runCatching { URLDecoder.decode(rawKey, StandardCharsets.UTF_8.name()) }.getOrDefault(rawKey)
            val value = runCatching { URLDecoder.decode(rawVal, StandardCharsets.UTF_8.name()) }.getOrDefault(rawVal)
            if (key.isNotEmpty()) key to value else null
        }
    }
}

@Composable
private fun QueryParamsSection(params: List<Pair<String, String>>) {
    if (params.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
            contentAlignment = Alignment.Center
        ) {
            Text("(无 Query 参数)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
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
            params.forEach { (key, value) ->
                HeaderRow(key = key, value = value)
            }
        }
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
