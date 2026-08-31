package sophon.desktop.feature.packetcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json

/**
 * JSON 纯文本多行编辑器。
 *
 * 适用于直接录入、复制粘贴完整 JSON，或修复畸形 JSON。
 * 支持等宽字体、垂直滚动、实时语法校验与状态提示。
 */
@Composable
internal fun JsonRawTextEditor(
    jsonText: String,
    onJsonChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val parseError = remember(jsonText) {
        if (jsonText.isBlank()) null
        else runCatching { Json.parseToJsonElement(jsonText) }.exceptionOrNull()?.localizedMessage
    }

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFFAFAFA), RoundedCornerShape(4.dp))
                .border(
                    width = 1.dp,
                    color = if (parseError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            BasicTextField(
                value = jsonText,
                onValueChange = onJsonChange,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (jsonText.isEmpty()) {
                        Text(
                            text = "请输入或粘贴 JSON 内容...",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (parseError != null) {
                Text(
                    text = "JSON 格式错误: $parseError",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = if (jsonText.isBlank()) "空 JSON" else "✓ JSON 格式正确",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (jsonText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
