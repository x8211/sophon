package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import sophon.desktop.feature.packetcapture.model.GrpcMockRule
import java.util.UUID

/**
 * gRPC Mock 规则管理对话框。
 *
 * 展示规则列表（启用/禁用开关 + 删除）；
 * 点击「新增规则」或某条规则后在底部展开编辑表单（host / path / JSON / grpc-status）；
 * 保存时触发 [onSave]，由 ViewModel 负责编码与持久化。
 */
@Composable
fun GrpcMockDialog(
    rules: List<GrpcMockRule>,
    encodeErrors: Map<String, String>,
    editingRule: GrpcMockRule?,
    onSave: (GrpcMockRule) -> Unit,
    onDelete: (String) -> Unit,
    onToggle: (String) -> Unit,
    onStartEdit: (GrpcMockRule?) -> Unit,
    onDismiss: () -> Unit,
) {
    // 将表单状态提升到 Dialog 层，以便 confirmButton 访问
    var formHost   by remember(editingRule?.id) { mutableStateOf(editingRule?.host        ?: "") }
    var formPath   by remember(editingRule?.id) { mutableStateOf(editingRule?.path        ?: "") }
    var formJson   by remember(editingRule?.id) { mutableStateOf(editingRule?.responseJson ?: "{}") }
    val canSave = editingRule != null && formHost.isNotBlank() && formPath.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("gRPC Mock 规则") },
        text = {
            // 编辑态：fillMaxHeight 响应窗口高度；非编辑态：包裹内容高度
            Column(
                modifier = if (editingRule != null)
                    Modifier.fillMaxWidth().fillMaxHeight(0.85f)
                else
                    Modifier.fillMaxWidth()
            ) {
                // 规则列表（固定区域，内部可滚动）
                if (rules.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (editingRule != null) 120.dp else 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        rules.forEach { rule ->
                            MockRuleRow(
                                rule = rule,
                                encodeError = encodeErrors[rule.id],
                                isEditing = editingRule?.id == rule.id,
                                onToggle = { onToggle(rule.id) },
                                onEdit = { onStartEdit(rule) },
                                onDelete = { onDelete(rule.id) },
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 编辑表单：weight(1f) 占据剩余高度
                if (editingRule != null) {
                    MockRuleEditForm(
                        modifier             = Modifier.weight(1f),
                        host                 = formHost,   onHostChange = { formHost = it },
                        path                 = formPath,   onPathChange = { formPath = it },
                        responseJson         = formJson,   onJsonChange = { formJson = it },
                        isNew                = editingRule.host.isBlank() && editingRule.path.isBlank(),
                    )
                } else {
                    TextButton(
                        onClick = {
                            onStartEdit(GrpcMockRule(id = UUID.randomUUID().toString(), host = "", path = ""))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增规则", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            if (editingRule != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onStartEdit(null) }) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                editingRule.copy(
                                    host         = formHost.trim(),
                                    path         = formPath.trim(),
                                    responseJson = formJson,
                                )
                            )
                        },
                        enabled = canSave,
                    ) { Text("保存") }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

@Composable
private fun MockRuleRow(
    rule: GrpcMockRule,
    encodeError: String?,
    isEditing: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusColor = when {
        encodeError != null -> MaterialTheme.colorScheme.error
        rule.enabled        -> MaterialTheme.colorScheme.primary
        else                -> MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEditing) Modifier.background(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩小 Switch 以保持行高紧凑
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.7f).size(width = 52.dp, height = 32.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.host.ifBlank { "*" },
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = rule.path.ifBlank { "(未设置路径)" },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            // 状态指示点（纯装饰，不可交互）
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (encodeError != null) {
            Text(
                text = encodeError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

private enum class JsonEditorMode(val label: String) {
    TREE("树形"),
    RAW("纯文本"),
}

private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
private val compactJson = Json { prettyPrint = false; ignoreUnknownKeys = true }

@Composable
private fun MockRuleEditForm(
    modifier: Modifier = Modifier,
    host: String,         onHostChange: (String) -> Unit,
    path: String,         onPathChange: (String) -> Unit,
    responseJson: String, onJsonChange: (String) -> Unit,
    isNew: Boolean,
) {
    var editorMode by remember { mutableStateOf(JsonEditorMode.TREE) }

    Column(modifier = modifier) {
        HorizontalDivider(thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isNew) "新增规则" else "编辑规则",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = host,
            onValueChange = onHostChange,
            label = { Text("Host（* 表示通配所有）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            placeholder = { Text("api.example.com", style = MaterialTheme.typography.bodySmall) }
        )
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = path,
            onValueChange = onPathChange,
            label = { Text("gRPC Path") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            placeholder = { Text("/pkg.Service/Method", style = MaterialTheme.typography.bodySmall) }
        )
        Spacer(Modifier.height(6.dp))

        // 响应 JSON 标题栏 + 工具栏（格式化、压缩）+ 模式切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "响应 JSON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        runCatching {
                            val elem = Json.parseToJsonElement(responseJson)
                            onJsonChange(prettyJson.encodeToString(JsonElement.serializer(), elem))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("格式化", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                }
                TextButton(
                    onClick = {
                        runCatching {
                            val elem = Json.parseToJsonElement(responseJson)
                            onJsonChange(compactJson.encodeToString(JsonElement.serializer(), elem))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("压缩", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                }

                Spacer(Modifier.width(4.dp))

                // 树形 / 纯文本 切换 pill
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JsonEditorMode.entries.forEach { mode ->
                        val isSelected = editorMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                )
                                .clickable { editorMode = mode }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // JSON 编辑区域（树形 or 纯文本）
        when (editorMode) {
            JsonEditorMode.TREE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    JsonTreeEditor(jsonText = responseJson, onJsonChange = onJsonChange)
                }
            }
            JsonEditorMode.RAW -> {
                JsonRawTextEditor(
                    jsonText = responseJson,
                    onJsonChange = onJsonChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
