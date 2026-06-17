package sophon.desktop.feature.packetcapture.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.packetcapture.ui.components.CaptureToolbar
import sophon.desktop.feature.packetcapture.ui.components.EmptyDetailPanel
import sophon.desktop.feature.packetcapture.ui.components.HostTreePanel
import sophon.desktop.feature.packetcapture.ui.components.PacketDetailPanel
import sophon.desktop.feature.packetcapture.ui.components.ProtoSchemaDialog
import sophon.desktop.feature.packetcapture.ui.components.ThrottleDialog

/**
 * 抓包功能主屏幕，Charles Proxy 风格布局：
 * - 顶部工具栏（开始/停止/清空/CA 安装/设备代理）
 * - 左侧 Host 树形面板（260dp，可折叠分组 + 底部过滤框）
 * - 右侧请求详情面板（URL 摘要行 + 概览/内容 两级标签）
 */
@Composable
fun PacketCaptureScreen(
    viewModel: PacketCaptureViewModel = viewModel { PacketCaptureViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CaptureToolbar(
            status = state.status,
            deviceProxy = state.deviceProxy,
            throttleConfig = state.throttleConfig,
            onStart = { viewModel.startCapture() },
            onStop = { viewModel.stopCapture() },
            onClear = { viewModel.clearPackets() },
            onInstallCa = { viewModel.installCaToDevice() },
            onOpenProtoManager = { viewModel.openProtoManager() },
            onOpenThrottleDialog = { viewModel.openThrottleDialog() },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            HostTreePanel(
                groupedPackets = state.groupedPackets,
                expandedHosts = state.expandedHosts,
                selectedPacketId = state.selectedPacketId,
                filterText = state.filterText,
                onToggleHost = { viewModel.toggleHostExpanded(it) },
                onSelectPacket = { viewModel.selectPacket(it) },
                onFilterChange = { viewModel.updateFilter(it) },
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
            )

            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            val selectedPacket = state.selectedPacket
            if (selectedPacket != null) {
                PacketDetailPanel(
                    packet = selectedPacket,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else {
                EmptyDetailPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }

    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("错误") },
            text = { Text(state.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("确定") }
            }
        )
    }

    if (state.showCaInstallGuide) {
        CaInstallGuideDialog(onDismiss = { viewModel.dismissCaInstallGuide() })
    }

    if (state.showThrottleDialog) {
        ThrottleDialog(
            current = state.throttleConfig,
            onConfirm = { viewModel.updateThrottle(it) },
            onDismiss = { viewModel.closeThrottleDialog() },
        )
    }

    if (state.showProtoManager) {
        ProtoSchemaDialog(
            protoPaths = state.protoPaths,
            schemaLoadedCount = state.schemaLoadedCount,
            schemaLoadError = state.schemaLoadError,
            onAddProtoPath = { path, isDir -> viewModel.addProtoPath(path, isDir) },
            onRemoveProtoPath = { viewModel.removeProtoPath(it) },
            onReload = { viewModel.reloadProtoSchema() },
            onDismiss = { viewModel.closeProtoManager() }
        )
    }
}

@Composable
private fun CaInstallGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CA 证书安装指引") },
        text = {
            Text(
                "CA 证书文件已推送到设备 /sdcard/MicoToolboxCA.crt。\n\n" +
                        "请在 Android 设备上操作：\n" +
                        "设置 → 安全 → 加密和凭据 → 安装证书 → CA 证书\n\n" +
                        "找到 /sdcard/MicoToolboxCA.crt 并安装。\n\n" +
                        "提示：部分设备需先设置锁屏密码，且证书安装后需信任才能生效。",
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}
