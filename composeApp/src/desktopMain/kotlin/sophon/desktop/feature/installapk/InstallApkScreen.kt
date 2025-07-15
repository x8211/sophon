package sophon.desktop.feature.installapk

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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot
import sophon.desktop.ui.components.FileChooser
import sophon.desktop.ui.components.OutputConsole

/**
 * 安装APK页面
 */
@Slot("安装APK")
class InstallApkScreen : Screen {

    @Composable
    override fun Content() {
        val installVM = rememberScreenModel { InstallApkViewModel() }
        val output by installVM.state.collectAsState()

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FileChooser(
                    title = "安装APK",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    content = ""
                ) { installVM.installApk(it) }

                Spacer(modifier = Modifier.height(24.dp))

                OutputConsole(
                    output = output,
                    onClear = { /* 不需要清除功能 */ },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }


}

