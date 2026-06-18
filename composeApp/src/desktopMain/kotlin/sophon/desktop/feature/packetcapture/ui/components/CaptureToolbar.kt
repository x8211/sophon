package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import sophon.composeapp.generated.resources.Res
import sophon.composeapp.generated.resources.ic_ca_certificate
import sophon.composeapp.generated.resources.ic_protobuf
import sophon.desktop.feature.packetcapture.model.CaptureStatus
import sophon.desktop.feature.packetcapture.model.ThrottleConfig
import sophon.desktop.feature.packetcapture.model.ThrottlePreset
import sophon.desktop.ui.theme.AppTheme
import sophon.desktop.ui.theme.Dimens

/**
 * 抓包工具栏，提供开始/停止、清空列表、CA 证书安装及限速配置的操作入口，
 * 并展示当前设备代理地址，按钮颜色随 [CaptureStatus] 联动变化。
 * 过滤框已下移至左侧树形面板底部（Charles 风格）。
 */
@Composable
fun CaptureToolbar(
    status: CaptureStatus,
    throttleConfig: ThrottleConfig,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onInstallCa: () -> Unit,
    onOpenProtoManager: () -> Unit,
    onOpenThrottleDialog: () -> Unit,
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
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "停止",
                        modifier = Modifier.size(Dimens.iconSizeSmall)
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onStart,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "开始",
                        modifier = Modifier.size(Dimens.iconSizeSmall)
                    )
                }
            }

            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "清空",
                    modifier = Modifier.size(Dimens.iconSizeSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilterChip(
                selected = throttleConfig.isActive,
                onClick = onOpenThrottleDialog,
                label = {
                    Text(
                        if (throttleConfig.isActive) throttleConfig.displayLabel else "不限速",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.NetworkCheck,
                        contentDescription = "限速",
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )

            Spacer(Modifier.weight(1f))

            TextButton(onClick = onInstallCa) {
                Icon(
                    painter = painterResource(Res.drawable.ic_ca_certificate),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text("安装CA证书", style = MaterialTheme.typography.labelSmall)
            }

            TextButton(onClick = onOpenProtoManager) {
                Icon(
                    painter = painterResource(Res.drawable.ic_protobuf),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text("添加Proto文件", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}


@Preview
@Composable
private fun CaptureToolbarStoppedPreview() {
    AppTheme {
        CaptureToolbar(
            status = CaptureStatus.STOPPED,
            throttleConfig = ThrottleConfig(),
            onStart = {},
            onStop = {},
            onClear = {},
            onInstallCa = {},
            onOpenProtoManager = {},
            onOpenThrottleDialog = {},
        )
    }
}

@Preview
@Composable
private fun CaptureToolbarRunningPreview() {
    AppTheme {
        CaptureToolbar(
            status = CaptureStatus.RUNNING,
            throttleConfig = ThrottleConfig(preset = ThrottlePreset.FAST_3G),
            onStart = {},
            onStop = {},
            onClear = {},
            onInstallCa = {},
            onOpenProtoManager = {},
            onOpenThrottleDialog = {},
        )
    }
}
