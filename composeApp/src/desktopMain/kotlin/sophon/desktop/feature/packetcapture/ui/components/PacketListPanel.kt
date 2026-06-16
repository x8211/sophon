package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sophon.desktop.feature.packetcapture.model.CapturedPacket

private val methodColors = mapOf(
    "GET" to Color(0xFF2196F3),
    "POST" to Color(0xFF4CAF50),
    "PUT" to Color(0xFFFF9800),
    "DELETE" to Color(0xFFF44336),
    "PATCH" to Color(0xFF9C27B0),
    "HEAD" to Color(0xFF00BCD4),
    "OPTIONS" to Color(0xFF607D8B),
)

private fun statusColor(statusCode: Int?): Color = when {
    statusCode == null -> Color(0xFF9E9E9E)
    statusCode < 300 -> Color(0xFF4CAF50)
    statusCode < 400 -> Color(0xFFFF9800)
    statusCode < 500 -> Color(0xFFF44336)
    else -> Color(0xFF9C27B0)
}

@Composable
fun PacketListPanel(
    packets: List<CapturedPacket>,
    selectedPacketId: Long?,
    onSelectPacket: (CapturedPacket) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(packets.size) {
        if (packets.isNotEmpty()) {
            listState.animateScrollToItem(packets.size - 1)
        }
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        if (packets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "暂无数据\n等待 HTTP/HTTPS 流量...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxHeight()) {
                items(packets, key = { it.id }) { packet ->
                    PacketListItem(
                        packet = packet,
                        isSelected = packet.id == selectedPacketId,
                        onClick = { onSelectPacket(packet) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                }
            }
        }
    }
}

@Composable
private fun PacketListItem(
    packet: CapturedPacket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = packet.method,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            ),
            color = methodColors[packet.method] ?: Color(0xFF607D8B),
            modifier = Modifier.width(44.dp)
        )

        Spacer(Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = packet.host,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = packet.path,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = packet.statusText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = statusColor(packet.statusCode),
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        if (packet.durationMs != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${packet.durationMs}ms",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color(0xFF9E9E9E),
                modifier = Modifier.width(42.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
