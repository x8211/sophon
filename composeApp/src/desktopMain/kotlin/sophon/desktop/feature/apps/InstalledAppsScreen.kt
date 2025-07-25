package sophon.desktop.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot
import sophon.desktop.ui.theme.MaaIcons

@Slot("已安装应用")
class InstalledAppsScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { InstalledAppsViewModel() }

        LaunchedEffect(Unit) {
            viewModel.loadApps()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (viewModel.isLoading) {
                // 加载进度显示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在解析应用列表...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = viewModel.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已解析 ${(viewModel.progress * viewModel.totalApps).toInt()} / ${viewModel.totalApps} 个应用",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // 双列布局
                Row(modifier = Modifier.fillMaxSize()) {
                    // 左侧应用列表 (1/3宽度)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.apps) { app ->
                            AppListItem(
                                app = app,
                                isSelected = app.packageName == viewModel.currentApp?.packageName,
                                onClick = { viewModel.selectApp(app) }
                            )
                        }
                    }
                    
                    // 右侧应用详情 (2/3宽度)
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        if (viewModel.currentApp != null) {
                            AppDetailsContent(app = viewModel.currentApp!!)
                        } else {
                            // 未选择应用时的提示
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "请从左侧列表选择一个应用查看详情",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppListItem(
        app: AppInfo,
        isSelected: Boolean,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 应用图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    app.icon?.let { icon ->
                        Image(
                            bitmap = icon,
                            contentDescription = app.appName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } ?: run {
                        // 如果没有图标，显示默认图标
                        Icon(
                            Icons.Default.Android,
                            contentDescription = app.appName,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 应用名称
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 包名
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun AppDetailsContent(app: AppInfo) {
        // 重用了AppDetailsScreen的内容，但不包含Scaffold和顶部栏
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 应用标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 信息部分
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
        }
    }

    @Composable
    private fun InfoSection(title: String, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            content()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    private fun InfoItem(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(120.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}