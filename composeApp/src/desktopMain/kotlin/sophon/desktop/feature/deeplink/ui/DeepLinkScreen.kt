package sophon.desktop.feature.deeplink.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.ui.components.DefaultListItem
import sophon.desktop.ui.components.OutputConsole
import sophon.desktop.ui.components.SectionTitle

/**
 * DeepLink 测试页面
 * 功能完全参考Deeplink Tester
 */
@Composable
fun DeepLinkScreen(
    viewModel: DeepLinkViewModel = viewModel { DeepLinkViewModel() }
) {
    val output by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState()
    var uri by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Input Area
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uri,
                    onValueChange = { uri = it },
                    label = { Text("Deep Link 链接") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = { viewModel.openDeepLink(uri) }) {
                    Text("打开")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Middle: History List
            HistoryList(
                history = history,
                onRun = {
                    uri = it
                    viewModel.openDeepLink(it)
                },
                onCopy = { clipboardManager.setText(AnnotatedString(it)) },
                onDelete = { viewModel.deleteHistory(it) },
                onFill = { uri = it },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom: Output Console
            OutputConsole(
                output = output,
                onClear = { viewModel.clearOutput() },
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    history: List<String>,
    onRun: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (String) -> Unit,
    onFill: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        // 分类标题
        SectionTitle("历史记录", modifier = Modifier.fillMaxWidth())

        // List
        LazyColumn {
            items(history) { link ->
                DefaultListItem(title = link, onClick = { onFill(link) }) {
                    Row {
                        IconButton(
                            onClick = { onRun(link) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "运行",
                                tint = Color.Green
                            )
                        }
                        IconButton(
                            onClick = { onCopy(link) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                tint = Color.LightGray
                            )
                        }
                        IconButton(
                            onClick = { onDelete(link) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color.Red.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
