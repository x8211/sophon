package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sophon.desktop.feature.packetcapture.model.CaptureStatus

/**
 * 抓包工具栏，提供开始/停止、清空列表及 CA 证书安装的操作入口，
 * 并展示当前设备代理地址，按钮颜色随 [CaptureStatus] 联动变化。
 * 过滤框已下移至左侧树形面板底部（Charles 风格）。
 */
@Composable
fun CaptureToolbar(
    status: CaptureStatus,
    deviceProxy: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onInstallCa: () -> Unit,
    onOpenProtoManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status == CaptureStatus.RUNNING) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, "停止", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("停止", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, "开始", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("开始", style = MaterialTheme.typography.labelMedium)
                }
            }

            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "清空",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            if (deviceProxy.isNotBlank()) {
                Text(
                    "设备代理: $deviceProxy",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (status) {
                        CaptureStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        CaptureStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            TextButton(onClick = onInstallCa) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("安装CA证书", style = MaterialTheme.typography.labelSmall)
            }

            IconButton(onClick = onOpenProtoManager) {
                Icon(
                    Icons.Filled.Code,
                    contentDescription = "管理 Proto Schema",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
