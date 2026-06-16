package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
    } catch (e: Exception) {
        text
    }
}

private fun isJson(contentType: String?): Boolean =
    contentType?.contains("json", ignoreCase = true) == true

/**
 * 请求详情面板，以标签页形式分区展示选中数据包的概览、请求头/体及响应头/体。
 * JSON 格式的 Body 自动美化显示，支持横向与纵向双向滚动。
 */
@Composable
fun PacketDetailPanel(
    packet: CapturedPacket,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("概览", "请求头", "请求体", "响应头", "响应体")
    var selectedTab by remember(packet.id) { mutableIntStateOf(0) }
    val reqContentType = packet.requestHeaders["Content-Type"]
        ?: packet.requestHeaders["content-type"]
    val respContentType = packet.responseHeaders["Content-Type"]
        ?: packet.responseHeaders["content-type"]

    Column(modifier = modifier) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(title, style = MaterialTheme.typography.labelMedium)
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
        ) {
            when (selectedTab) {
                0 -> OverviewTab(packet)
                1 -> HeadersTab(packet.requestHeaders)
                2 -> BodyTab(packet.requestBodyAsText(), isJson(reqContentType))
                3 -> HeadersTab(packet.responseHeaders)
                4 -> BodyTab(packet.responseBodyAsText(), isJson(respContentType))
            }
        }
    }
}

@Composable
private fun OverviewTab(packet: CapturedPacket) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        OverviewRow("方法", packet.method)
        OverviewRow("URL", packet.url)
        OverviewRow("协议", packet.scheme.uppercase())
        OverviewRow("状态码", packet.statusCode?.toString() ?: "-")
        OverviewRow("耗时", packet.durationMs?.let { "${it}ms" } ?: "-")
        OverviewRow("请求大小", "${packet.requestBodySize()} bytes")
        OverviewRow("响应大小", "${packet.responseBodySize()} bytes")
        if (packet.error != null) {
            OverviewRow("错误", packet.error, valueColor = Color(0xFFF44336))
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

@Composable
private fun HeadersTab(headers: Map<String, String>) {
    val scrollState = rememberScrollState()
    if (headers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("(无请求头)", color = Color(0xFF9E9E9E))
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        headers.entries.sortedBy { it.key }.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
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
    }
}

@Composable
private fun BodyTab(bodyText: String?, isJson: Boolean) {
    val text = if (isJson && bodyText != null) formatJsonOrRaw(bodyText) else bodyText ?: "(空)"
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
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
