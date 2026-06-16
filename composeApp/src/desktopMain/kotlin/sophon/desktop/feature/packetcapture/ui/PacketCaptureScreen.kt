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
import sophon.desktop.feature.packetcapture.ui.components.PacketDetailPanel
import sophon.desktop.feature.packetcapture.ui.components.PacketListPanel

@Composable
fun PacketCaptureScreen(
    viewModel: PacketCaptureViewModel = viewModel { PacketCaptureViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CaptureToolbar(
        status = state.status,
        filterText = state.filterText,
            deviceProxy = state.deviceProxy,
            onStart = { viewModel.startCapture() },
            onStop = { viewModel.stopCapture() },
            onClear = { viewModel.clearPackets() },
            onFilterChange = { viewModel.updateFilter(it) },
            onInstallCa = { viewModel.installCaToDevice() },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PacketListPanel(
                packets = state.filteredPackets,
                selectedPacketId = state.selectedPacketId,
                onSelectPacket = { viewModel.selectPacket(it) },
                modifier = Modifier
                    .width(360.dp)
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
