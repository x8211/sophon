package sophon.desktop.feature.deeplink

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot
import sophon.desktop.ui.components.OutputConsole

/**
 * DeepLink 测试页面
 * 功能完全参考Deeplink Tester
 */
@Slot("DeepLink")
class DeepLinkScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { DeepLinkViewModel() }
        val output by viewModel.state.collectAsState()
        val history by viewModel.history.collectAsState()
        var uri by remember { mutableStateOf("") }
        val clipboardManager = LocalClipboardManager.current

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    
                    Button(
                        onClick = { viewModel.openDeepLink(uri) }
                    ) {
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
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom: Output Console
                OutputConsole(
                    output = output,
                    onClear = { viewModel.clearOutput() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    placeholder = "运行结果将显示在这里"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    history: List<String>,
    onRun: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (String) -> Unit,
    onFill: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF212121) // Match OutputConsole dark theme look
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF333333))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "历史记录",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }

            // List
            LazyColumn(
                modifier = Modifier.weight(1f).padding(8.dp)
            ) {
                items(history) { link ->
                    TooltipArea(
                        tooltip = {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Text(link, modifier = Modifier.padding(8.dp))
                            }
                        }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    link, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable { onFill(link) }
                                .padding(vertical = 4.dp),
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onRun(link) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "运行", tint = Color.Green)
                                    }
                                    IconButton(onClick = { onCopy(link) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "复制", tint = Color.LightGray)
                                    }
                                    IconButton(onClick = { onDelete(link) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
