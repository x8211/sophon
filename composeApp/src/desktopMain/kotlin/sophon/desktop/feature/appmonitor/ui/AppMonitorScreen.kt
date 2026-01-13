package sophon.desktop.feature.appmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.appmonitor.feature.activitystack.ui.ActivityStackScreen
import sophon.desktop.feature.appmonitor.feature.fileexplorer.ui.FileExplorerScreen
import sophon.desktop.feature.appmonitor.feature.thread.ui.ThreadScreen
import sophon.desktop.ui.theme.Dimens

/**
 * 应用监控主界面
 *
 * 整合线程信息、文件浏览器、Activity栈三个子功能
 * 布局结构：
 * 1. 顶部：紧凑的应用信息栏（包名和debuggable状态）
 * 2. 中部：TabLayout子功能切换
 * 3. 底部：子功能内容区域（占据大部分空间）
 */
@Composable
fun AppMonitorScreen(
    viewModel: AppMonitorViewModel = viewModel { AppMonitorViewModel() }
) {
    val appInfo by viewModel.appInfo.collectAsState()
    val selectedFeature by viewModel.selectedFeature.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val refreshTrigger by viewModel.refreshTrigger.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPolling()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Top
    ) {
        // 顶部：紧凑的应用信息栏
        CompactAppInfoBar(
            packageName = appInfo?.packageName,
            isDebuggable = appInfo?.isDebuggable,
            errorMessage = errorMessage
        )

        // 中部：TabLayout子功能选择
        FeatureTabRow(
            selectedFeature = selectedFeature,
            onFeatureSelected = { viewModel.selectFeature(it) }
        )

        // 底部：子功能内容区域（占据剩余所有空间）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 将当前应用包名和刷新触发器传递给子功能
            val packageName = appInfo?.packageName
            when (selectedFeature) {
                AppMonitorFeature.THREAD -> ThreadScreen(
                    packageName = packageName,
                    refreshTrigger = refreshTrigger
                )

                AppMonitorFeature.FILE_EXPLORER -> FileExplorerScreen(packageName = packageName)
                AppMonitorFeature.ACTIVITY_STACK -> ActivityStackScreen(
                    packageName = packageName,
                    refreshTrigger = refreshTrigger
                )
            }
        }
    }
}

/**
 * 紧凑的应用信息栏
 *
 * 单行显示应用包名和debuggable状态，减少垂直空间占用
 *
 * @param packageName 应用包名
 * @param isDebuggable 是否为debuggable模式
 * @param errorMessage 错误信息
 */
@Composable
private fun CompactAppInfoBar(
    packageName: String?,
    isDebuggable: Boolean?,
    errorMessage: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Dimens.paddingMedium, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            errorMessage != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "错误",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(2.dp)
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            packageName != null -> {
                // 左侧：包名
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 右侧：Debuggable状态
                Surface(
                    color =
                        if (isDebuggable == true) Color(0xFF16A34A).copy(alpha = 0.1f) // 绿色背景
                        else Color(0xFFF97316).copy(alpha = 0.1f), // 橙色背景
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector =
                                if (isDebuggable == true) Icons.Default.CheckCircle
                                else Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint =
                                if (isDebuggable == true) Color(0xFF16A34A) // 绿色
                                else Color(0xFFF97316) // 橙色
                        )
                        Text(
                            text =
                                if (isDebuggable == true) "Debuggable"
                                else "Release",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color =
                                if (isDebuggable == true) Color(0xFF16A34A)
                                else Color(0xFFF97316)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 子功能TabRow
 *
 * 使用Material Design 3的TabRow实现子功能切换
 *
 * @param selectedFeature 当前选中的子功能
 * @param onFeatureSelected 子功能选择回调
 */
@Composable
private fun FeatureTabRow(
    selectedFeature: AppMonitorFeature,
    onFeatureSelected: (AppMonitorFeature) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedFeature.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        AppMonitorFeature.entries.forEach { feature ->
            Tab(
                selected = selectedFeature == feature,
                onClick = { onFeatureSelected(feature) },
                text = {
                    Text(
                        text = feature.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )
        }
    }
}