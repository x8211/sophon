package sophon.desktop.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sophon.desktop.core.datastore.adbDataStore
import sophon.desktop.generated.AppInfo
import sophon.desktop.ui.components.DefaultListItem

/**
 * 设置页面
 * 模仿 Android 系统设置样式的生动页面，符合 Material Design 3 规范。
 */
@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    val adbConfig by adbDataStore.data.collectAsState(initial = null)
    val scrollState = rememberScrollState()

    var showAdbDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        DefaultListItem(
            title = "ADB 配置",
            description = adbConfig?.toolPath?.takeIf { it.isNotEmpty() }
                ?: "未设置自定义路径",
            icon = Icons.Default.Terminal,
            onClick = { showAdbDialog = true }
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        DefaultListItem(
            title = "应用版本号",
            description = AppInfo.APP_VERSION,
            icon = null
        )

        DefaultListItem(
            title = "应用打包时间",
            description = AppInfo.BUILD_TIME,
            icon = null
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAdbDialog) {
        AdbPathEditDialog(
            initialPath = adbConfig?.toolPath ?: "",
            onDismiss = { showAdbDialog = false },
            onConfirm = { newPath ->
                scope.launch {
                    adbDataStore.updateData { it.copy(toolPath = newPath) }
                }
                showAdbDialog = false
            }
        )
    }
}

/**
 * ADB 路径编辑弹窗
 */
@Composable
private fun AdbPathEditDialog(
    initialPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var path by remember { mutableStateOf(initialPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 ADB 路径") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Adb 可执行文件路径") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "指定 ADB 工具的完整路径（例如：/usr/local/bin/adb）。留空则使用系统默认路径。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(path) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
