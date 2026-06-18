package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sophon.desktop.feature.packetcapture.model.CapturedPacket

private val grpcTreeColor = Color(0xFF7B1FA2)

private val methodTreeColors = mapOf(
    "GET" to Color(0xFF2196F3),
    "POST" to Color(0xFF4CAF50),
    "PUT" to Color(0xFFFF9800),
    "DELETE" to Color(0xFFF44336),
    "PATCH" to Color(0xFF9C27B0),
    "HEAD" to Color(0xFF00BCD4),
    "OPTIONS" to Color(0xFF607D8B),
)

private fun treeStatusColor(statusCode: Int?): Color = when {
    statusCode == null -> Color(0xFF9E9E9E)
    statusCode < 300 -> Color(0xFF4CAF50)
    statusCode < 400 -> Color(0xFFFF9800)
    statusCode < 500 -> Color(0xFFF44336)
    else -> Color(0xFF9C27B0)
}

/**
 * Charles Proxy 风格的 host 树形面板。
 *
 * 按 host 将请求分组，每个 host 组可独立折叠/展开；
 * 展开时以精简行显示各条请求（path + status + duration）；
 * 底部固定过滤框，同步 ViewModel 中的 filterText。
 */
@Composable
fun HostTreePanel(
    groupedPackets: Map<String, List<CapturedPacket>>,
    expandedHosts: Set<String>,
    selectedPacketId: Long?,
    filterText: String,
    onToggleHost: (String) -> Unit,
    onSelectPacket: (CapturedPacket) -> Unit,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize()) {

            if (groupedPackets.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    groupedPackets.forEach { (host, packets) ->
                        val isExpanded = host in expandedHosts

                        // Host 分组头
                        stickyHeader(key = "header_$host") {
                            HostGroupHeader(
                                host = host,
                                packetCount = packets.size,
                                isExpanded = isExpanded,
                                onClick = { onToggleHost(host) }
                            )
                        }

                        // 展开时显示该 host 下的请求列表
                        if (isExpanded) {
                            items(packets, key = { it.id }) { packet ->
                                PacketTreeItem(
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

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 底部固定过滤框
            OutlinedTextField(
                value = filterText,
                onValueChange = onFilterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                placeholder = {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { onFilterChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun HostGroupHeader(
    host: String,
    packetCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = host,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = packetCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PacketTreeItem(
    packet: CapturedPacket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(
                if (isSelected) Modifier.drawBehind {
                    drawRect(color = accentColor, size = Size(6f, size.height))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(start = if (isSelected) 34.dp else 28.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 协议/方法标记
        val methodLabel = if (packet is CapturedPacket.Grpc) "gRPC" else packet.method
        val methodColor = if (packet is CapturedPacket.Grpc) grpcTreeColor
        else methodTreeColors[packet.method] ?: Color(0xFF607D8B)

        Text(
            text = methodLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            ),
            color = methodColor,
            modifier = Modifier.width(36.dp)
        )

        Spacer(Modifier.width(4.dp))

        // 路径（gRPC 显示服务/方法名）
        val pathLabel = if (packet is CapturedPacket.Grpc)
            listOfNotNull(packet.service, packet.rpcMethod).joinToString("/").ifEmpty { packet.path }
        else packet.path

        Text(
            text = pathLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            ),
            color = if (packet is CapturedPacket.Grpc) grpcTreeColor.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(4.dp))

        // 状态码
        Text(
            text = packet.statusText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            ),
            color = treeStatusColor(packet.statusCode),
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        // 耗时
        if (packet.durationMs != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${packet.durationMs}ms",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color(0xFF9E9E9E),
                modifier = Modifier.width(38.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
