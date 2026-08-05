package sophon.desktop.feature.update.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.Dimens

/**
 * 顶部更新提示 Banner。
 *
 * 仅在有新版本（[UpdateUiState.NewVersion]）、下载中（[UpdateUiState.Downloading]）
 * 或下载完成（[UpdateUiState.ReadyToInstall]）时可见。
 */
@Composable
fun UpdateBanner(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val visible = state is UpdateUiState.NewVersion
            || state is UpdateUiState.Downloading
            || state is UpdateUiState.ReadyToInstall

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = Dimens.paddingLarge, vertical = Dimens.paddingMedium)
        ) {
            when (val s = state) {
                is UpdateUiState.NewVersion -> NewVersionRow(
                    version = s.info.version,
                    onUpdate = { viewModel.downloadAndInstall(s.info) },
                    onIgnore = { viewModel.ignoreVersion(s.info.version) },
                    onDismiss = { viewModel.dismiss() }
                )

                is UpdateUiState.Downloading -> DownloadingRow(progress = s.progress)

                is UpdateUiState.ReadyToInstall -> ReadyToInstallRow(onDismiss = { viewModel.dismiss() })

                else -> {}
            }
        }
    }
}

@Composable
private fun NewVersionRow(
    version: String,
    onUpdate: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
    ) {
        Icon(
            Icons.Default.SystemUpdateAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(Dimens.iconSizeMedium)
        )
        Text(
            text = "新版本 $version 可用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onUpdate) {
            Text("立即更新", color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = onIgnore) {
            Text(
                "忽略此版本",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Dimens.iconSizeSmall)
            )
        }
    }
}

@Composable
private fun DownloadingRow(progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.iconSizeMedium),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (progress < 0f) "正在下载更新…" else "正在下载更新…${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        if (progress >= 0f) {
            Spacer(Modifier.width(Dimens.paddingMedium))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(120.dp).height(4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ReadyToInstallRow(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
    ) {
        Icon(
            Icons.Default.SystemUpdateAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(Dimens.iconSizeMedium)
        )
        Text(
            text = "安装包已就绪，请按提示完成安装",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Dimens.iconSizeSmall)
            )
        }
    }
}
