package sophon.desktop.feature.installaab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.ui.components.FileChooser
import sophon.desktop.ui.components.OutputConsole
import sophon.desktop.ui.components.openFileChooser
import sophon.desktop.ui.theme.Dimens
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun InstallAabScreen(viewModel: InstallAabViewModel = viewModel { InstallAabViewModel() }) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacerMedium)
    ) {
        // AAB 文件选择区
        FileChooser(
            modifier = Modifier.height(100.dp).fillMaxWidth(),
            content = state.aabPath.ifBlank { "点击或拖拽 .aab 文件到此" },
            fileSelectionMode = JFileChooser.FILES_ONLY,
            fileFilter = FileNameExtensionFilter("Android App Bundle (*.aab)", "aab"),
            onFileSelected = { viewModel.onAabPathSelected(it) },
        )

        // 签名配置区（可折叠）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // 折叠标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onKeystoreExpandedToggle() }
                    .padding(horizontal = Dimens.paddingLarge, vertical = Dimens.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("签名配置", style = MaterialTheme.typography.titleSmall)
                Icon(
                    imageVector = if (state.isKeystoreExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (state.isKeystoreExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = state.isKeystoreExpanded) {
                Column(
                    modifier = Modifier.padding(
                        start = Dimens.paddingLarge,
                        end = Dimens.paddingLarge,
                        bottom = Dimens.paddingLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacerSmall)
                ) {
                    // Keystore 路径
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.keystorePath,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Keystore 路径") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(Dimens.spacerSmall))
                        IconButton(
                            onClick = {
                                val path = openFileChooser(
                                    fileSelectionMode = JFileChooser.FILES_ONLY,
                                    fileFilter = FileNameExtensionFilter(
                                        "Keystore 文件 (*.jks, *.keystore)",
                                        "jks",
                                        "keystore"
                                    ),
                                )
                                viewModel.onKeystorePathSelected(path)
                            }
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "选择 Keystore",
                                modifier = Modifier.size(Dimens.iconSizeMedium)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.storePassword,
                        onValueChange = { viewModel.onStorePasswordChange(it) },
                        label = { Text("Store 密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    OutlinedTextField(
                        value = state.keyAlias,
                        onValueChange = { viewModel.onKeyAliasChange(it) },
                        label = { Text("Key Alias") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = state.keyPassword,
                        onValueChange = { viewModel.onKeyPasswordChange(it) },
                        label = { Text("Key 密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
        }

        // 安装按钮
        Button(
            onClick = { viewModel.installAab() },
            enabled = state.aabPath.isNotBlank() && !state.isInstalling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.iconSizeSmall),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(Dimens.spacerSmall))
                Text("安装中...")
            } else {
                Text("安装 AAB")
            }
        }

        // 输出控制台
        OutputConsole(
            output = state.output,
            onClear = { viewModel.clearOutput() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}
