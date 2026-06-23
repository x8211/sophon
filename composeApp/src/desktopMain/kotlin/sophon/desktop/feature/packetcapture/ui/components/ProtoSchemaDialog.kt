package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sophon.desktop.feature.packetcapture.model.ProtoPath
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * gRPC Schema 管理对话框。
 *
 * 允许用户通过系统文件选择器添加包含单个或多个 `.proto` 的文件夹，
 * 路径列表持久化由 ViewModel 负责。
 */
@Composable
fun ProtoSchemaDialog(
    protoPaths: List<ProtoPath>,
    schemaLoadedCount: Int,
    schemaLoadError: String?,
    onAddProtoPath: (path: String, isDirectory: Boolean) -> Unit,
    onRemoveProtoPath: (path: String) -> Unit,
    onReload: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("gRPC Schema 管理") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                // 已加载路径列表
                Text(
                    "已配置路径：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                ) {
                    if (protoPaths.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无路径\n点击下方按钮添加包含 .proto 文件的文件夹",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn {
                            items(protoPaths, key = { it.path }) { pp ->
                                ProtoPathItem(
                                    protoPath = pp,
                                    onRemove = { onRemoveProtoPath(pp.path) }
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 添加按钮行
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val dir = pickDirectory()
                            dir?.let { onAddProtoPath(it, true) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加文件夹", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(8.dp))

                // 加载状态
                if (schemaLoadError != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = schemaLoadError,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .heightIn(max = 160.dp)
                                .verticalScroll(scrollState)
                                .padding(10.dp)
                        )
                    }
                } else {
                    val statusText = when {
                        schemaLoadedCount < 0 -> "未加载（添加路径后点击重新加载）"
                        schemaLoadedCount == 0 -> "未找到可用 Schema（请检查 .proto 文件格式）"
                        else -> "已加载 $schemaLoadedCount 个消息 Schema"
                    }
                    val statusColor = when {
                        schemaLoadedCount > 0 -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReload) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("重新加载")
                }
                Button(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
}

@Composable
private fun ProtoPathItem(protoPath: ProtoPath, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (protoPath.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (protoPath.isDirectory) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = protoPath.path,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ─── 文件/目录选择器（JVM Swing）──────────────────────────────────────────────

/**
 * 打开系统文件选择器，允许多选 `.proto` 文件。
 * 必须在协程中调用（suspend），在 Swing EDT 上执行以避免线程问题。
 */
private suspend fun pickFiles(): List<String> = withContext(Dispatchers.IO) {
    var result: List<String> = emptyList()
    val latch = java.util.concurrent.CountDownLatch(1)
    SwingUtilities.invokeLater {
        try {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                isMultiSelectionEnabled = true
                dialogTitle = "选择 .proto 文件"
                fileFilter = FileNameExtensionFilter("Proto 文件 (*.proto)", "proto")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFiles.map { it.absolutePath }
            }
        } finally {
            latch.countDown()
        }
    }
    latch.await()
    result
}

/**
 * 打开系统目录选择器，返回选中目录路径。
 */
private suspend fun pickDirectory(): String? = withContext(Dispatchers.IO) {
    var result: String? = null
    val latch = java.util.concurrent.CountDownLatch(1)
    SwingUtilities.invokeLater {
        try {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "选择包含 .proto 文件的目录"
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFile.absolutePath
            }
        } finally {
            latch.countDown()
        }
    }
    latch.await()
    result
}
