package sophon.desktop.feature.systemmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.systemmonitor.feature.camera.ui.CameraScreen
import sophon.desktop.feature.systemmonitor.feature.cpu.ui.CpuScreen
import sophon.desktop.feature.systemmonitor.feature.temperature.ui.TemperatureScreen
import sophon.desktop.ui.theme.Dimens

/**
 * 系统监控主界面
 *
 * 整合温度监测、图形监测、CPU监测三个子功能
 * 布局结构：
 * 1. 顶部：系统信息栏（如有错误则显示）
 * 2. 中部：TabLayout子功能切换
 * 3. 底部：子功能内容区域（占据大部分空间）
 */
@Composable
fun SystemMonitorScreen(
    viewModel: SystemMonitorViewModel = viewModel { SystemMonitorViewModel() }
) {
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
        // 顶部：系统信息栏（仅在有错误时显示）
        if (errorMessage != null) {
            SystemInfoBar(errorMessage = errorMessage)
        }

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
            // 将刷新触发器传递给子功能
            when (selectedFeature) {
                SystemMonitorFeature.TEMPERATURE -> TemperatureScreen(refreshTrigger)
                SystemMonitorFeature.CPU_MONITOR -> CpuScreen(refreshTrigger)
                SystemMonitorFeature.CAMERA_MONITOR -> CameraScreen(refreshTrigger)
            }
        }
    }
}

/**
 * 系统信息栏
 *
 * 显示错误信息
 *
 * @param errorMessage 错误信息
 */
@Composable
private fun SystemInfoBar(
    errorMessage: String?
) {
    if (errorMessage != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = Dimens.paddingMedium, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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
    selectedFeature: SystemMonitorFeature,
    onFeatureSelected: (SystemMonitorFeature) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedFeature.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        SystemMonitorFeature.entries.forEach { feature ->
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

/**
 * 系统监控子功能枚举
 */
enum class SystemMonitorFeature(val displayName: String) {
    TEMPERATURE("温度监测"),
    CPU_MONITOR("CPU监测"),
    CAMERA_MONITOR("相机监测")
}
