package sophon.desktop.feature.installapk.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.ui.components.FileChooser
import sophon.desktop.ui.components.OutputConsole

/**
 * 安装APK页面
 */
@Composable
fun InstallApkScreen(viewModel: InstallApkViewModel = viewModel { InstallApkViewModel() }) {
    val output by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FileChooser(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                content = ""
            ) { viewModel.installApk(it) }

            Spacer(modifier = Modifier.height(24.dp))

            OutputConsole(
                output = output,
                onClear = { /* 不需要清除功能 */ },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}
