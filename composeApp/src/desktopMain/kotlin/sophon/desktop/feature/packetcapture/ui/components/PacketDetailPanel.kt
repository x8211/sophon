package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
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
import kotlinx.serialization.json.JsonElement
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

/**
 * Charles Proxy 风格的请求详情面板。
 *
 * 顶部：URL 摘要行（method → url → status）。
 * 主标签页：概览 / 内容。
 * 内容页内部垂直分割：请求头（上，weight=0.4）+ 响应格式选择标签（中）+ 响应体（下，weight=0.6）。
 */
@Composable
fun PacketDetailPanel(
    packet: CapturedPacket,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(packet.id) { mutableIntStateOf(0) }

    Column(modifier = modifier) {

        // --- URL 摘要行 ---
        RequestSummaryBar(packet)

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // --- 主标签页 ---
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
// 概览 Tab（保持现有内容）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(packet: CapturedPacket) {
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
// 内容 Tab（Charles 风格：请求头 上 + 响应 下）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContentsTab(packet: CapturedPacket) {
    // 响应区格式子标签：0=响应头 1=文本 2=JSON 3=原始
    var responseTab by remember(packet.id) { mutableIntStateOf(
        // 若响应体是 JSON，默认选 JSON 子标签
        if (isJson(packet.responseHeaders["Content-Type"] ?: packet.responseHeaders["content-type"])) 2 else 1
    ) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {

        // 请求头区域（上，weight=0.4）
        Column(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "请求头",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            RequestHeadersSection(packet.requestHeaders)
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // 响应格式选择标签行（中间分隔条）
        ScrollableTabRow(
            selectedTabIndex = responseTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            divider = {}
        ) {
            listOf("响应头", "文本", "JSON", "原始").forEachIndexed { index, title ->
                Tab(
                    selected = responseTab == index,
                    onClick = { responseTab = index },
                    text = {
                        Text(title, style = MaterialTheme.typography.labelSmall)
                    }
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // 响应内容区域（下，weight=0.6）
        Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            when (responseTab) {
                0 -> ResponseHeadersSection(packet.responseHeaders)
                1 -> BodySection(packet.responseBodyAsText(), formatAsJson = false)
                2 -> BodySection(packet.responseBodyAsText(), formatAsJson = true)
                3 -> BodySection(packet.responseBodyAsText(), formatAsJson = false)
            }
        }
    }
}

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

@Composable
private fun BodySection(bodyText: String?, formatAsJson: Boolean) {
    val text = when {
        bodyText.isNullOrEmpty() -> "(空)"
        formatAsJson -> formatJsonOrRaw(bodyText)
        else -> bodyText
    }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
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
