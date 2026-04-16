package sophon.desktop.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sophon.desktop.core.CACHE_HOME
import sophon.desktop.generated.AppInfo
import sophon.desktop.ui.components.DefaultListItem

/**
 * 设置页面
 * 模仿 Android 系统设置样式的生动页面，符合 Material Design 3 规范。
 */
@Composable
fun SettingsScreen() {
    val scrollState = rememberScrollState()

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
        )

        DefaultListItem(
            title = "应用资源路径",
            description = System.getProperty("compose.application.resources.dir")?:"Unknown",
        )

        DefaultListItem(
            title = "应用打包时间",
            description = AppInfo.BUILD_TIME,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
