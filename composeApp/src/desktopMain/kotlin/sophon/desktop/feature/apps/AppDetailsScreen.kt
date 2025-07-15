package sophon.desktop.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.MaaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    app: AppInfo,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.appName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = MaaIcons.Error, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 应用标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                app.icon?.let { icon ->
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Divider()

            // 基本信息
            InfoSection("基本信息") {
                InfoItem("包名", app.packageName)
                InfoItem("版本", "${app.versionName} (${app.versionCode})")
                InfoItem("安装位置", app.path)
                InfoItem("大小", "${app.size / 1024 / 1024}MB")
                InfoItem("数据大小", "${app.dataSize / 1024 / 1024}MB")
                InfoItem("缓存大小", "${app.cacheSize / 1024 / 1024}MB")
            }

            // SDK信息
            InfoSection("SDK信息") {
                InfoItem("最低SDK版本", app.minSdkVersion)
                InfoItem("目标SDK版本", app.targetSdkVersion)
                InfoItem("编译SDK版本", app.compileSdkVersion)
                InfoItem("构建工具版本", app.buildToolsVersion)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        content()
        Divider()
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}