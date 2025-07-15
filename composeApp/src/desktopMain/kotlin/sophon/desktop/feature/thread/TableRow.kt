package sophon.desktop.feature.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableRow(modifier: Modifier = Modifier, vararg items: TableRowItem) {
    Row(modifier = modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
        repeat(items.size) {
            val item = items[it]
            
            // 所有单元格使用相同的基础布局结构
            Box(
                modifier = Modifier
                    .weight(item.weight)
                    .border(0.5.dp, Color.Black)
            ) {
                if (item.tooltip.isNotEmpty()) {
                    var showTooltip by remember { mutableStateOf(false) }
                    
                    TooltipArea(
                        tooltip = {
                            // 添加延迟显示效果
                            LaunchedEffect(true) {
                                showTooltip = false
                                delay(500) // 悬停500毫秒后显示tooltip
                                showTooltip = true
                            }
                            
                            if (showTooltip) {
                                Surface(
                                    modifier = Modifier.shadow(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.tooltip,
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        },
                        tooltipPlacement = TooltipPlacement.CursorPoint(
                            alignment = Alignment.BottomStart,
                            offset = DpOffset(0.dp, 20.dp)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            item.text,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        item.text,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// 保留tooltip字段，但暂时不使用
data class TableRowItem(val text: String, val weight: Float = 1f, val tooltip: String = "")

fun String.toTableRowItem(weight: Float = 1f, tooltip: String = "") = TableRowItem(this, weight, tooltip)