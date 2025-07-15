package sophon.desktop.feature.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot

/**
 * 开发者选项页面
 */
@Slot("开发者选项")
class DeveloperScreen : Screen {
    @Composable
    override fun Content() {
        val developerVM = rememberScreenModel { DeveloperViewModel() }
        val uiState by developerVM.uiState.collectAsState()

        var showWindowScaleDialog by remember { mutableStateOf(false) }
        var showTransitionScaleDialog by remember { mutableStateOf(false) }
        var showAnimatorScaleDialog by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            item {
                CategorySection(
                    title = "调试",
                    options = debugOptions(uiState, developerVM)
                )
            }

            item {
                CategorySection(
                    title = "输入",
                    options = inputOptions(uiState, developerVM)
                )
            }

            item {
                CategorySection(
                    title = "绘图",
                    options = drawingOptions(uiState, developerVM)
                )
            }

            item {
                CategorySection(
                    title = "动画",
                    options = listOf(
                        DeveloperOption(
                            title = "窗口动画缩放",
                            description = "窗口打开和关闭的动画速度调整",
                            showAsScale = true,
                            scaleValue = uiState.windowAnimationScale,
                            onScaleClick = { showWindowScaleDialog = true }
                        ),
                        DeveloperOption(
                            title = "过渡动画缩放",
                            description = "应用程序切换时的动画速度调整",
                            showAsScale = true,
                            scaleValue = uiState.transitionAnimationScale,
                            onScaleClick = { showTransitionScaleDialog = true }
                        ),
                        DeveloperOption(
                            title = "Animator时长缩放",
                            description = "应用程序内的动画速度调整",
                            showAsScale = true,
                            scaleValue = uiState.animatorDurationScale,
                            onScaleClick = { showAnimatorScaleDialog = true }
                        )
                    )
                )
            }

            item {
                CategorySection(
                    title = "监控",
                    options = monitoringOptions(uiState, developerVM)
                )
            }
        }

        if (showWindowScaleDialog) {
            AnimationScaleDialog(
                title = "窗口动画缩放",
                currentScale = uiState.windowAnimationScale,
                onScaleSelected = { scale -> developerVM.setWindowAnimationScale(scale) },
                onDismiss = { showWindowScaleDialog = false }
            )
        }

        if (showTransitionScaleDialog) {
            AnimationScaleDialog(
                title = "过渡动画缩放",
                currentScale = uiState.transitionAnimationScale,
                onScaleSelected = { scale -> developerVM.setTransitionAnimationScale(scale) },
                onDismiss = { showTransitionScaleDialog = false }
            )
        }

        if (showAnimatorScaleDialog) {
            AnimationScaleDialog(
                title = "Animator时长缩放",
                currentScale = uiState.animatorDurationScale,
                onScaleSelected = { scale -> developerVM.setAnimatorDurationScale(scale) },
                onDismiss = { showAnimatorScaleDialog = false }
            )
        }
    }

    @Composable
    private fun CategorySection(
        title: String,
        options: List<DeveloperOption>,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 分类标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE3F2FD))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1A1A1A)
                )
            }

            // 选项列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                options.forEachIndexed { index, option ->
                    SwitchItem(option)
                    if (index < options.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFEEEEEE),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SwitchItem(option: DeveloperOption) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF1A1A1A)
                )
                if (option.description.isNotBlank()) {
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))

            if (option.showAsScale) {
                AnimationScaleButton(
                    value = option.scaleValue,
                    onClick = option.onScaleClick
                )
            } else {
                Switch(
                    checked = option.checked,
                    onCheckedChange = option.onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color(0xFFBDBDBD),
                        uncheckedTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }
        }
    }

    @Composable
    private fun AnimationScaleButton(value: Float, onClick: () -> Unit) {
        TextButton(onClick = onClick) {
            Text(
                text = "${value}x",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private data class DeveloperOption(
    val title: String,
    val description: String = "",
    val checked: Boolean = false,
    val onCheckedChange: (Boolean) -> Unit = {},
    val showAsScale: Boolean = false,
    val scaleValue: Float = 1.0f,
    val onScaleClick: () -> Unit = {},
)

private fun debugOptions(uiState: DeveloperUiState, viewModel: DeveloperViewModel) = listOf(
    DeveloperOption(
        title = "布局边界",
        description = "显示所有视图的布局边界",
        checked = uiState.debugLayout,
        onCheckedChange = { viewModel.toggleDebugLayout() }
    ),
    DeveloperOption(
        title = "严格模式",
        description = "在主线程上检测耗时操作",
        checked = uiState.strictMode,
        onCheckedChange = { viewModel.toggleStrictMode() }
    ),
    DeveloperOption(
        title = "不保留活动",
        description = "关闭后台活动以节省内存，离开活动后立即销毁",
        checked = uiState.dontKeepActivities,
        onCheckedChange = { viewModel.toggleDontKeepActivities() }
    ),
    DeveloperOption(
        title = "显示所有ANR",
        description = "显示所有应用程序无响应对话框",
        checked = uiState.showAllANRs,
        onCheckedChange = { viewModel.toggleShowAllANRs() }
    )
)

private fun inputOptions(uiState: DeveloperUiState, viewModel: DeveloperViewModel) = listOf(
    DeveloperOption(
        title = "显示触摸操作",
        description = "在屏幕上显示触摸操作",
        checked = uiState.showTouches,
        onCheckedChange = { viewModel.toggleShowTouches() }
    ),
    DeveloperOption(
        title = "指针位置",
        description = "显示触摸坐标",
        checked = uiState.pointerLocation,
        onCheckedChange = { viewModel.togglePointerLocation() }
    ),
    DeveloperOption(
        title = "强制RTL布局",
        description = "强制从右到左的布局方向",
        checked = uiState.forceRtl,
        onCheckedChange = { viewModel.toggleForceRtl() }
    )
)

private fun drawingOptions(uiState: DeveloperUiState, viewModel: DeveloperViewModel) = listOf(
    DeveloperOption(
        title = "GPU呈现模式分析",
        description = "显示GPU渲染时间",
        checked = uiState.hwUi,
        onCheckedChange = { viewModel.toggleHwUi() }
    )
)

private fun monitoringOptions(uiState: DeveloperUiState, viewModel: DeveloperViewModel) = listOf(
    DeveloperOption(
        title = "保持唤醒状态",
        description = "充电时保持屏幕常亮",
        checked = uiState.stayAwake,
        onCheckedChange = { viewModel.toggleStayAwake() }
    )
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AnimationScaleDialog(
    title: String,
    currentScale: Float,
    onScaleSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Column {
                val scales = listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 5.0f, 10.0f)
                scales.forEach { scale ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onScaleSelected(scale)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${scale}x",
                                color = if (scale == currentScale)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (scale == currentScale)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // 空的确认按钮，使用列表项目作为选择
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}