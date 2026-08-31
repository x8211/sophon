package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

// ─────────────────────────────────────────────────────────────────────────────
// JSON 折叠树视图（共享组件）
// ─────────────────────────────────────────────────────────────────────────────

private val jsonMonoStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )

/**
 * JSON 折叠树视图（接受预解析的 [JsonElement]）。
 * 由后台线程预解析后传入，避免在主线程做 JSON 解析。
 */
@Composable
internal fun JsonTreeView(element: JsonElement?, modifier: Modifier = Modifier) {
    if (element == null) {
        Box(modifier.fillMaxSize().background(Color(0xFFFAFAFA)), contentAlignment = Alignment.Center) {
            Text("(空)", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    SelectionContainer {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            JsonNodeView(key = null, element = element, depth = 0)
        }
    }
}

/**
 * JSON 折叠树视图（接受 JSON 字符串，在 Composable 中解析）。
 * 自动剥离 gRPC 帧头前缀（`[压缩: ... bytes]\n`）。
 * 解析失败时显示错误提示。
 */
@Composable
internal fun JsonTreeView(jsonText: String, modifier: Modifier = Modifier) {
    val cleanJson = remember(jsonText) {
        if (jsonText.startsWith("[")) jsonText.substringAfter('\n') else jsonText
    }
    val element = remember(cleanJson) {
        runCatching { Json.parseToJsonElement(cleanJson) }.getOrNull()
    }

    if (element == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .padding(12.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = if (jsonText.isBlank()) "(空)" else "JSON 格式错误，无法渲染预览",
                color = Color(0xFF9E9E9E),
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    SelectionContainer {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            JsonNodeView(key = null, element = element, depth = 0)
        }
    }
}

@Composable
private fun JsonNodeView(key: String?, element: JsonElement, depth: Int) {
    when (element) {
        is JsonObject -> JsonObjectView(key = key, obj = element, depth = depth)
        is JsonArray -> JsonArrayView(key = key, arr = element, depth = depth)
        is JsonPrimitive -> JsonPrimitiveRow(key = key, primitive = element, depth = depth)
    }
}

@Composable
private fun JsonObjectView(key: String?, obj: JsonObject, depth: Int) {
    var expanded by remember { mutableStateOf(depth == 0) }
    val startPadding = (depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "▼ " else "▶ ",
            style = jsonMonoStyle,
            color = Color(0xFF9E9E9E)
        )
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        if (expanded) {
            Text("{", style = jsonMonoStyle, color = Color(0xFF616161))
        } else {
            Text("{ ${obj.size} }", style = jsonMonoStyle, color = Color(0xFF9E9E9E))
        }
    }

    if (expanded) {
        obj.entries.forEach { (k, v) ->
            JsonNodeView(key = k, element = v, depth = depth + 1)
        }
        Row(modifier = Modifier.padding(start = startPadding + 16.dp, top = 1.dp, bottom = 1.dp)) {
            Text("}", style = jsonMonoStyle, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun JsonArrayView(key: String?, arr: JsonArray, depth: Int) {
    var expanded by remember { mutableStateOf(depth == 0) }
    val startPadding = (depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "▼ " else "▶ ",
            style = jsonMonoStyle,
            color = Color(0xFF9E9E9E)
        )
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        if (expanded) {
            Text("[", style = jsonMonoStyle, color = Color(0xFF616161))
        } else {
            Text("[ ${arr.size} ]", style = jsonMonoStyle, color = Color(0xFF9E9E9E))
        }
    }

    if (expanded) {
        arr.forEach { element ->
            JsonNodeView(key = null, element = element, depth = depth + 1)
        }
        Row(modifier = Modifier.padding(start = startPadding + 16.dp, top = 1.dp, bottom = 1.dp)) {
            Text("]", style = jsonMonoStyle, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun JsonPrimitiveRow(key: String?, primitive: JsonPrimitive, depth: Int) {
    val startPadding = (depth * 16).dp + 16.dp

    val (valueText, valueColor) = when {
        primitive is JsonNull -> "null" to Color(0xFF9E9E9E)
        primitive.isString -> "\"${primitive.content}\"" to Color(0xFF2E7D32)
        primitive.content == "true" -> "true" to Color(0xFF1565C0)
        primitive.content == "false" -> "false" to Color(0xFFC62828)
        else -> primitive.content to Color(0xFFBF360C)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        Text(valueText, style = jsonMonoStyle, color = valueColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JSON 内联编辑树（JsonTreeEditor）
// ─────────────────────────────────────────────────────────────────────────────

private val editorPrettyJson = Json { prettyPrint = true }

/** 标识 JSON 树中某个节点的路径（从根节点出发的步骤序列）。 */
private sealed class PathSeg {
    data class Key(val name: String) : PathSeg()
    data class Idx(val index: Int) : PathSeg()
}

private data class InlineEditState(
    val path: List<PathSeg>,
    val textValue: TextFieldValue,
    val hasError: Boolean = false,
)

/**
 * JSON 内联编辑树。
 *
 * 叶子节点（原始值）可点击直接修改：点击值 → 内联 [BasicTextField] 弹出 → 按 Enter 提交 / Esc 取消。
 * 修改后通过 [onJsonChange] 回调传出更新后的 JSON 字符串。
 * 自动剥离 gRPC 帧头前缀。JSON 无效时显示提示。
 */
@Composable
internal fun JsonTreeEditor(
    jsonText: String,
    onJsonChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cleanJson = remember(jsonText) {
        if (jsonText.startsWith("[")) jsonText.substringAfter('\n') else jsonText
    }
    val root = remember(cleanJson) {
        runCatching { Json.parseToJsonElement(cleanJson) }.getOrNull()
    }

    if (root == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .padding(12.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = if (jsonText.isBlank()) "(空，请在原始编辑模式中输入 JSON)" else "JSON 格式错误，请切换到原始编辑模式",
                color = Color(0xFF9E9E9E),
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    var editState by remember { mutableStateOf<InlineEditState?>(null) }

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        JsonNodeEditorView(
            key = null,
            element = root,
            depth = 0,
            path = emptyList(),
            editState = editState,
            onEditStart = { path, initialText ->
                editState = InlineEditState(
                    path = path,
                    textValue = TextFieldValue(initialText, TextRange(0, initialText.length)),
                )
            },
            onEditTextChange = { newValue ->
                editState = editState?.copy(textValue = newValue, hasError = false)
            },
            onEditCommit = { path, rawText ->
                val newPrimitive = parseRawValue(rawText)
                if (newPrimitive != null) {
                    val updated = root.updateAtPath(path, newPrimitive)
                    val newJson = runCatching {
                        editorPrettyJson.encodeToString(JsonElement.serializer(), updated)
                    }.getOrNull()
                    if (newJson != null) onJsonChange(newJson)
                    editState = null
                } else {
                    editState = editState?.copy(hasError = true)
                }
            },
            onEditCancel = { editState = null },
        )
    }
}

@Composable
private fun JsonNodeEditorView(
    key: String?,
    element: JsonElement,
    depth: Int,
    path: List<PathSeg>,
    editState: InlineEditState?,
    onEditStart: (List<PathSeg>, String) -> Unit,
    onEditTextChange: (TextFieldValue) -> Unit,
    onEditCommit: (List<PathSeg>, String) -> Unit,
    onEditCancel: () -> Unit,
) {
    when (element) {
        is JsonObject -> JsonObjectEditorView(key, element, depth, path, editState, onEditStart, onEditTextChange, onEditCommit, onEditCancel)
        is JsonArray  -> JsonArrayEditorView(key, element, depth, path, editState, onEditStart, onEditTextChange, onEditCommit, onEditCancel)
        is JsonPrimitive -> JsonPrimitiveEditorRow(key, element, depth, path, editState, onEditStart, onEditTextChange, onEditCommit, onEditCancel)
    }
}

@Composable
private fun JsonObjectEditorView(
    key: String?,
    obj: JsonObject,
    depth: Int,
    path: List<PathSeg>,
    editState: InlineEditState?,
    onEditStart: (List<PathSeg>, String) -> Unit,
    onEditTextChange: (TextFieldValue) -> Unit,
    onEditCommit: (List<PathSeg>, String) -> Unit,
    onEditCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(depth == 0) }
    val startPadding = (depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (expanded) "▼ " else "▶ ", style = jsonMonoStyle, color = Color(0xFF9E9E9E))
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        Text(
            text = if (expanded) "{" else "{ ${obj.size} }",
            style = jsonMonoStyle,
            color = if (expanded) Color(0xFF616161) else Color(0xFF9E9E9E)
        )
    }

    if (expanded) {
        obj.entries.forEach { (k, v) ->
            JsonNodeEditorView(
                key = k, element = v, depth = depth + 1,
                path = path + PathSeg.Key(k),
                editState = editState,
                onEditStart = onEditStart, onEditTextChange = onEditTextChange,
                onEditCommit = onEditCommit, onEditCancel = onEditCancel,
            )
        }
        Row(modifier = Modifier.padding(start = startPadding + 16.dp, top = 1.dp, bottom = 1.dp)) {
            Text("}", style = jsonMonoStyle, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun JsonArrayEditorView(
    key: String?,
    arr: JsonArray,
    depth: Int,
    path: List<PathSeg>,
    editState: InlineEditState?,
    onEditStart: (List<PathSeg>, String) -> Unit,
    onEditTextChange: (TextFieldValue) -> Unit,
    onEditCommit: (List<PathSeg>, String) -> Unit,
    onEditCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(depth == 0) }
    val startPadding = (depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (expanded) "▼ " else "▶ ", style = jsonMonoStyle, color = Color(0xFF9E9E9E))
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }
        Text(
            text = if (expanded) "[" else "[ ${arr.size} ]",
            style = jsonMonoStyle,
            color = if (expanded) Color(0xFF616161) else Color(0xFF9E9E9E)
        )
    }

    if (expanded) {
        arr.forEachIndexed { i, element ->
            JsonNodeEditorView(
                key = "[$i]", element = element, depth = depth + 1,
                path = path + PathSeg.Idx(i),
                editState = editState,
                onEditStart = onEditStart, onEditTextChange = onEditTextChange,
                onEditCommit = onEditCommit, onEditCancel = onEditCancel,
            )
        }
        Row(modifier = Modifier.padding(start = startPadding + 16.dp, top = 1.dp, bottom = 1.dp)) {
            Text("]", style = jsonMonoStyle, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun JsonPrimitiveEditorRow(
    key: String?,
    primitive: JsonPrimitive,
    depth: Int,
    path: List<PathSeg>,
    editState: InlineEditState?,
    onEditStart: (List<PathSeg>, String) -> Unit,
    onEditTextChange: (TextFieldValue) -> Unit,
    onEditCommit: (List<PathSeg>, String) -> Unit,
    onEditCancel: () -> Unit,
) {
    val startPadding = (depth * 16).dp + 16.dp
    val isEditing = editState?.path == path

    val rawValueText = when {
        primitive is JsonNull -> "null"
        primitive.isString   -> "\"${primitive.content}\""
        else                 -> primitive.content
    }
    val valueColor = when {
        primitive is JsonNull            -> Color(0xFF9E9E9E)
        primitive.isString               -> Color(0xFF2E7D32)
        primitive.content == "true"      -> Color(0xFF1565C0)
        primitive.content == "false"     -> Color(0xFFC62828)
        else                             -> Color(0xFFBF360C)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isEditing) Modifier.clickable { onEditStart(path, rawValueText) } else Modifier)
            .padding(start = startPadding, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (key != null) {
            Text("\"$key\"", style = jsonMonoStyle, color = Color(0xFF0277BD))
            Text(": ", style = jsonMonoStyle, color = Color(0xFF616161))
        }

        if (isEditing) {
            val focusRequester = remember { FocusRequester() }
            val hasError = editState!!.hasError
            val borderColor = if (hasError) Color(0xFFD32F2F) else Color(0xFF0277BD)
            // 用 isEditing 作为 key，确保每次进入编辑态时 wasFocused 从 false 重置
            var wasFocused by remember(isEditing) { mutableStateOf(false) }

            BasicTextField(
                value = editState.textValue,
                onValueChange = onEditTextChange,
                textStyle = jsonMonoStyle.copy(color = if (hasError) Color(0xFFD32F2F) else valueColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onEditCommit(path, editState.textValue.text) }
                ),
                modifier = Modifier
                    .widthIn(min = 60.dp, max = 320.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            wasFocused = true
                        } else if (wasFocused) {
                            // 真正失焦（不是初始化触发），提交当前编辑
                            wasFocused = false
                            if (editState.path == path) {
                                onEditCommit(path, editState.textValue.text)
                            }
                        }
                    }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            onEditCancel(); true
                        } else false
                    }
                    .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )

            if (hasError) {
                Text(
                    text = " ← 无效值",
                    style = jsonMonoStyle,
                    color = Color(0xFFD32F2F),
                )
            }

            LaunchedEffect(path) { focusRequester.requestFocus() }
        } else {
            Text(
                text = rawValueText,
                style = jsonMonoStyle,
                color = valueColor,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 将 [path] 指定位置的值替换为 [newValue]，返回更新后的新 [JsonElement]（不可变操作）。
 */
private fun JsonElement.updateAtPath(path: List<PathSeg>, newValue: JsonPrimitive): JsonElement {
    if (path.isEmpty()) return newValue
    val head = path.first()
    val tail = path.drop(1)
    return when {
        this is JsonObject && head is PathSeg.Key -> buildJsonObject {
            this@updateAtPath.entries.forEach { (k, v) ->
                put(k, if (k == head.name) v.updateAtPath(tail, newValue) else v)
            }
        }
        this is JsonArray && head is PathSeg.Idx -> buildJsonArray {
            this@updateAtPath.forEachIndexed { i, v ->
                add(if (i == head.index) v.updateAtPath(tail, newValue) else v)
            }
        }
        else -> this
    }
}

/**
 * 将用户输入的原始文本解析为 JSON 原始值。
 *
 * 支持的格式：
 * - `null`
 * - `true` / `false`
 * - 整数 / 浮点数
 * - 带引号的字符串：`"hello"`
 * - 不带引号的字符串也被接受：`hello` → `"hello"`
 */
private fun parseRawValue(raw: String): JsonPrimitive? {
    val trimmed = raw.trim()
    return when {
        trimmed == "null"  -> JsonNull
        trimmed == "true"  -> JsonPrimitive(true)
        trimmed == "false" -> JsonPrimitive(false)
        trimmed.toLongOrNull() != null   -> JsonPrimitive(trimmed.toLong())
        trimmed.toDoubleOrNull() != null -> JsonPrimitive(trimmed.toDouble())
        trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2 ->
            JsonPrimitive(trimmed.drop(1).dropLast(1))
        trimmed.isNotEmpty() -> JsonPrimitive(trimmed)
        else -> null
    }
}
