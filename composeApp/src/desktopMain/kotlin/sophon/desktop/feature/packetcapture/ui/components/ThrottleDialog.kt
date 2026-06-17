package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import sophon.desktop.feature.packetcapture.model.ThrottleConfig
import sophon.desktop.feature.packetcapture.model.ThrottlePreset

/**
 * 限速配置对话框，展示预设档位列表与自定义速率输入框。
 * 选择"自定义"时显示下载/上传速率输入；确认后通过 [onConfirm] 回调结果。
 */
@Composable
fun ThrottleDialog(
    current: ThrottleConfig,
    onConfirm: (ThrottleConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPreset by remember { mutableStateOf(current.preset) }
    var downloadKbps by remember { mutableLongStateOf(current.customDownloadKbps) }
    var uploadKbps by remember { mutableLongStateOf(current.customUploadKbps) }
    var downloadText by remember { mutableStateOf(current.customDownloadKbps.toString()) }
    var uploadText by remember { mutableStateOf(current.customUploadKbps.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("网络限速") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ThrottlePreset.entries.forEach { preset ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (selectedPreset == ThrottlePreset.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = downloadText,
                            onValueChange = { v ->
                                downloadText = v
                                v.toLongOrNull()?.let { downloadKbps = it }
                            },
                            label = { Text("下载 (Kbps)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = uploadText,
                            onValueChange = { v ->
                                uploadText = v
                                v.toLongOrNull()?.let { uploadKbps = it }
                            },
                            label = { Text("上传 (Kbps)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = ThrottleConfig(
                        preset = selectedPreset,
                        customDownloadKbps = downloadKbps.coerceAtLeast(1L),
                        customUploadKbps = uploadKbps.coerceAtLeast(1L),
                    )
                    onConfirm(config)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
