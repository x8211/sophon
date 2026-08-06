package sophon.desktop.feature.packetcapture.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.packetcapture.ui.components.CaptureToolbar
import sophon.desktop.feature.packetcapture.ui.components.EmptyDetailPanel
import sophon.desktop.feature.packetcapture.ui.components.GrpcMockDialog
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
            throttleConfig = state.throttleConfig,
            mockRuleCount = state.mockRules.count { it.enabled },
            onStart = { viewModel.startCapture() },
            onStop = { viewModel.stopCapture() },
            onClear = { viewModel.clearPackets() },
            onInstallCa = { viewModel.installCaToDevice() },
            onOpenProtoManager = { viewModel.openProtoManager() },
            onOpenThrottleDialog = { viewModel.openThrottleDialog() },
            onOpenMockDialog = { viewModel.openMockDialog() },
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
                onAddMockFromPacket = { viewModel.addMockFromPacket(it) },
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            )

            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            val selectedPacket = state.selectedPacket
            if (selectedPacket != null) {
                PacketDetailPanel(
                    packet = selectedPacket,
                    decodedBody = state.decodedBodies[selectedPacket.id],
                    isDecodingBody = state.isDecodingBody,
                    onSaveFile = { viewModel.saveResponseBodyToFile(selectedPacket) },
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
            } else {
                EmptyDetailPanel(
                    modifier = Modifier
                        .weight(0.6f)
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
        CaInstallGuideDialog(
            caAndroidPushed = state.caAndroidPushed,
            caCertPath = state.caCertPath,
            onRevealInFinder = { viewModel.revealCaCertInFinder() },
            onDismiss = { viewModel.dismissCaInstallGuide() },
        )
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

    if (state.showMockDialog) {
        GrpcMockDialog(
            rules = state.mockRules,
            encodeErrors = state.mockEncodeErrors,
            editingRule = state.editingMockRule,
            onSave = { viewModel.saveMockRule(it) },
            onDelete = { viewModel.deleteMockRule(it) },
            onToggle = { viewModel.toggleMockRule(it) },
            onStartEdit = { viewModel.startEditMockRule(it) },
            onDismiss = { viewModel.closeMockDialog() }
        )
    }
}

@Composable
private fun CaInstallGuideDialog(
    caAndroidPushed: Boolean,
    caCertPath: String,
    onRevealInFinder: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Android", "iOS")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CA 证书安装指引") },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> AndroidCaGuideContent(caAndroidPushed)
                        1 -> IosCaGuideContent(caCertPath, onRevealInFinder)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun AndroidCaGuideContent(pushed: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pushed) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.size(6.dp))
            Text(
                text = if (pushed) "证书已通过 ADB 推送到 /sdcard/SophonCA.crt"
                else "未检测到已连接的 Android 设备，请手动推送证书：\nadb push <证书路径> /sdcard/SophonCA.crt",
                style = MaterialTheme.typography.bodySmall,
                color = if (pushed) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "安装步骤：",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "① 设置 → 安全 → 加密和凭据 → 安装证书 → CA 证书\n" +
            "② 找到 /sdcard/SophonCA.crt 并安装\n\n" +
            "提示：部分设备需先设置锁屏密码。安装后还需在\n" +
            "「受信任的凭据」→「用户」中确认证书已启用。",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "配置代理：\n设置 → 无线局域网 → 当前 Wi-Fi → 代理 → 手动\n填写 Mac 的局域网 IP，端口 8888",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IosCaGuideContent(caCertPath: String, onRevealInFinder: () -> Unit) {
    Column {
        Text(
            "iOS 抓包需手动配置代理并安装 CA 证书，完整步骤如下：",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        GuideStep(number = "1", title = "配置 Wi-Fi 代理") {
            Text(
                "iPhone/iPad 进入：设置 → 无线局域网\n" +
                "点击当前 Wi-Fi 右侧 ⓘ → 配置代理 → 手动\n" +
                "服务器填写 Mac 的局域网 IP，端口填写 8888",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(10.dp))

        GuideStep(number = "2", title = "在 Safari 中下载证书") {
            Text(
                "确保代理已配置后，在 iPhone 的 Safari 中访问：",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "http://sophon.cert",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "代理会自动拦截并返回证书，Safari 将提示下载配置文件。\n" +
                "（与 Charles 的 chls.pro/ssl 机制相同）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onRevealInFinder,
                modifier = Modifier.padding(0.dp)
            ) {
                Text("在 Finder 中显示证书", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(10.dp))

        GuideStep(number = "3", title = "安装描述文件") {
            Text(
                "iPhone 接收证书文件后：\n" +
                "设置 → 通用 → VPN 与设备管理\n" +
                "找到「Sophon CA」描述文件，点击「安装」",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(10.dp))

        GuideStep(number = "4", title = "开启完全信任") {
            Text(
                "设置 → 通用 → 关于本机 → 证书信任设置\n" +
                "找到「Sophon CA」并开启「针对根证书启用完全信任」",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GuideStep(number: String, title: String, content: @Composable () -> Unit) {
    Row {
        Text(
            text = number,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
                .then(
                    Modifier.drawCircleBadge(MaterialTheme.colorScheme.primary)
                ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            content()
        }
    }
}

private fun Modifier.drawCircleBadge(color: androidx.compose.ui.graphics.Color): Modifier =
    this.then(
        Modifier.drawBehind {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
    )
