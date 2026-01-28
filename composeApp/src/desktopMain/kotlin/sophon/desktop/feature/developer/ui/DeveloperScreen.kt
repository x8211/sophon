package sophon.desktop.feature.developer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.ui.components.SectionTitle
import sophon.desktop.ui.components.SwitchListItem
import sophon.desktop.ui.components.TrailingTextListItem

/**
 * 开发者选项页面
 */
@Composable
fun DeveloperScreen(viewModel: DeveloperViewModel = viewModel { DeveloperViewModel() }) {
    val uiState by viewModel.uiState.collectAsState()

    val showWindowScaleDialog = remember { mutableStateOf(false) }
    val showTransitionScaleDialog = remember { mutableStateOf(false) }
    val showAnimatorScaleDialog = remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            CategorySection(
                title = "调试",
                options = listOf(
                    DeveloperOptionItem.SwitchItem(
                        title = "布局边界",
                        description = "显示所有视图的布局边界",
                        checked = uiState.debugLayout,
                        onCheckedChange = { viewModel.toggleDebugLayout() }
                    ),
                    DeveloperOptionItem.SwitchItem(
                        title = "严格模式",
                        description = "在主线程上检测耗时操作",
                        checked = uiState.strictMode,
                        onCheckedChange = { viewModel.toggleStrictMode() }
                    ),
                    DeveloperOptionItem.SwitchItem(
                        title = "不保留活动",
                        description = "关闭后台活动以节省内存，离开活动后立即销毁",
                        checked = uiState.dontKeepActivities,
                        onCheckedChange = { viewModel.toggleDontKeepActivities() }
                    ),
                    DeveloperOptionItem.SwitchItem(
                        title = "显示所有ANR",
                        description = "显示所有应用程序无响应对话框",
                        checked = uiState.showAllANRs,
                        onCheckedChange = { viewModel.toggleShowAllANRs() }
                    )
                )
            )
        }

        item {
            CategorySection(
                title = "输入",
                options = listOf(
                    DeveloperOptionItem.SwitchItem(
                        title = "显示触摸操作",
                        description = "在屏幕上显示触摸操作",
                        checked = uiState.showTouches,
                        onCheckedChange = { viewModel.toggleShowTouches() }
                    ),
                    DeveloperOptionItem.SwitchItem(
                        title = "指针位置",
                        description = "显示触摸坐标",
                        checked = uiState.pointerLocation,
                        onCheckedChange = { viewModel.togglePointerLocation() }
                    ),
                    DeveloperOptionItem.SwitchItem(
                        title = "强制RTL布局",
                        description = "强制从右到左的布局方向",
                        checked = uiState.forceRtl,
                        onCheckedChange = { viewModel.toggleForceRtl() }
                    )
                )
            )
        }

        item {
            CategorySection(
                title = "绘图",
                options = listOf(
                    DeveloperOptionItem.SwitchItem(
                        title = "GPU呈现模式分析",
                        description = "显示GPU渲染时间",
                        checked = uiState.hwUi,
                        onCheckedChange = { viewModel.toggleHwUi() }
                    )
                )
            )
        }

        item {
            CategorySection(
                title = "动画",
                options = listOf(
                    DeveloperOptionItem.TextItem(
                        title = "窗口动画缩放",
                        description = "窗口打开和关闭的动画速度调整",
                        actionText = "${uiState.windowAnimationScale}x",
                        onClick = { showWindowScaleDialog.value = true }
                    ),
                    DeveloperOptionItem.TextItem(
                        title = "过渡动画缩放",
                        description = "应用程序切换时的动画速度调整",
                        actionText = "${uiState.transitionAnimationScale}x",
                        onClick = { showTransitionScaleDialog.value = true }
                    ),
                    DeveloperOptionItem.TextItem(
                        title = "Animator时长缩放",
                        description = "应用程序内的动画速度调整",
                        actionText = "${uiState.animatorDurationScale}x",
                        onClick = { showAnimatorScaleDialog.value = true }
                    )
                )
            )
        }

        item {
            CategorySection(
                title = "监控",
                options = listOf(
                    DeveloperOptionItem.SwitchItem(
                        title = "保持唤醒状态",
                        description = "充电时保持屏幕常亮",
                        checked = uiState.stayAwake,
                        onCheckedChange = { viewModel.toggleStayAwake() }
                    )
                )
            )
        }
    }

    if (showWindowScaleDialog.value) {
        AnimationScaleDialog(
            title = "窗口动画缩放",
            currentScale = uiState.windowAnimationScale,
            onScaleSelected = { scale -> viewModel.setWindowAnimationScale(scale) },
            onDismiss = { showWindowScaleDialog.value = false }
        )
    }

    if (showTransitionScaleDialog.value) {
        AnimationScaleDialog(
            title = "过渡动画缩放",
            currentScale = uiState.transitionAnimationScale,
            onScaleSelected = { scale -> viewModel.setTransitionAnimationScale(scale) },
            onDismiss = { showTransitionScaleDialog.value = false }
        )
    }

    if (showAnimatorScaleDialog.value) {
        AnimationScaleDialog(
            title = "Animator时长缩放",
            currentScale = uiState.animatorDurationScale,
            onScaleSelected = { scale -> viewModel.setAnimatorDurationScale(scale) },
            onDismiss = { showAnimatorScaleDialog.value = false }
        )
    }
}

@Composable
private fun CategorySection(
    title: String,
    options: List<DeveloperOptionItem>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 分类标题
        SectionTitle(title, modifier = Modifier.fillMaxWidth())

        // 选项列表
        Column(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                when (option) {
                    is DeveloperOptionItem.SwitchItem -> SwitchListItem(
                        option.title,
                        option.description,
                        option.checked,
                        modifier = Modifier.fillMaxWidth(),
                        onCheckedChange = option.onCheckedChange
                    )

                    is DeveloperOptionItem.TextItem -> TrailingTextListItem(
                        option.title,
                        option.description,
                        option.actionText,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = option.onClick
                    )
                }

                if (index < options.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFFEEEEEE),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}


sealed interface DeveloperOptionItem {
    data class TextItem(
        val title: String,
        val description: String = "",
        val actionText: String = "",
        val onClick: () -> Unit = {},
    ) : DeveloperOptionItem

    data class SwitchItem(
        val title: String,
        val description: String = "",
        val checked: Boolean = false,
        val onCheckedChange: (Boolean) -> Unit = {},
    ) : DeveloperOptionItem
}

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
