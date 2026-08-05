package sophon.desktop.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sophon.desktop.core.CACHE_HOME
import sophon.desktop.feature.update.ui.UpdateUiState
import sophon.desktop.feature.update.ui.UpdateViewModel
import sophon.desktop.generated.AppInfo
import sophon.desktop.ui.components.DefaultListItem
import sophon.desktop.ui.theme.Dimens
import java.awt.Desktop
import java.io.File

/**
 * 设置页面
 * 模仿 Android 系统设置样式的生动页面，符合 Material Design 3 规范。
 */
@Composable
fun SettingsScreen(updateViewModel: UpdateViewModel) {
    val scrollState = rememberScrollState()
    val updateState by updateViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        DefaultListItem(
            title = "应用版本号",
            description = AppInfo.APP_VERSION,
        )

        DefaultListItem(
            title = "缓存路径",
            description = CACHE_HOME,
            onClick = {
                val dir = File(CACHE_HOME).also { it.mkdirs() }
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir)
                }
            }
        )

        DefaultListItem(
            title = "应用打包时间",
            description = AppInfo.BUILD_TIME,
        )

        DefaultListItem(
            title = "检查更新",
            description = when (updateState) {
                is UpdateUiState.Checking -> "正在检查…"
                is UpdateUiState.UpToDate -> "已是最新版本"
                is UpdateUiState.NewVersion -> "发现新版本 ${(updateState as UpdateUiState.NewVersion).info.version}"
                is UpdateUiState.Downloading -> {
                    val p = (updateState as UpdateUiState.Downloading).progress
                    if (p < 0f) "正在下载…" else "正在下载… ${(p * 100).toInt()}%"
                }
                is UpdateUiState.ReadyToInstall -> "安装包已下载，请按提示完成安装"
                is UpdateUiState.Error -> "检查失败：${(updateState as UpdateUiState.Error).message}"
                else -> "当前版本 ${AppInfo.APP_VERSION}"
            },
            trailingContent = {
                UpdateActionContent(
                    state = updateState,
                    onCheck = { updateViewModel.checkForUpdate() },
                    onUpdate = {
                        val info = (updateState as? UpdateUiState.NewVersion)?.info ?: return@UpdateActionContent
                        updateViewModel.downloadAndInstall(info)
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun UpdateActionContent(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onUpdate: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (state) {
            is UpdateUiState.Checking, is UpdateUiState.Downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.iconSizeMedium),
                    strokeWidth = 2.dp
                )
            }

            is UpdateUiState.UpToDate -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconSizeMedium)
                )
                Spacer(Modifier.padding(start = Dimens.paddingMedium))
                Button(onClick = onCheck) { Text("重新检查") }
            }

            is UpdateUiState.NewVersion -> {
                Button(onClick = onUpdate) { Text("立即更新") }
            }

            is UpdateUiState.Error -> {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Dimens.iconSizeMedium)
                )
                Spacer(Modifier.padding(start = Dimens.paddingMedium))
                Button(onClick = onCheck) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSizeSmall)
                    )
                }
            }

            is UpdateUiState.ReadyToInstall -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconSizeMedium)
                )
            }

            else -> {
                Button(onClick = onCheck) { Text("检查更新") }
            }
        }
    }
}
