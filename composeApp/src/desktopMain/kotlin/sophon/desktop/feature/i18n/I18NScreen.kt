package sophon.desktop.feature.i18n

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot
import kotlinx.coroutines.delay
import sophon.desktop.ui.components.FileChooser
import sophon.desktop.ui.components.OutputConsole
import sophon.desktop.ui.theme.MaaIcons
import sophon.desktop.ui.theme.filledTonalButtonColorsMd3
import sophon.desktop.ui.theme.outlinedTextFieldColorsMd3
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * 多语言页面
 */
@Slot("多语言")
class I18NScreen : Screen {

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val i18nVM = rememberScreenModel { I18NViewModel() }
        val uiState by i18nVM.state.collectAsState()

        val project = uiState.project

        val currentStep = uiState.currentStep
        val selectedTabIndex = uiState.selectedTabIndex
        val listState = rememberLazyListState()

        // 步骤完成状态
        val stepOneCompleted by remember { derivedStateOf { uiState.toolPath.isNotBlank() } }
        val stepTwoCompleted by remember { derivedStateOf { uiState.csvPath.isNotBlank() } }
        val stepThreeCompleted by remember { derivedStateOf { uiState.modulePath.isNotBlank() } }

        // 进度条进度
        val progress by animateFloatAsState(
            targetValue = if (uiState.progress > 0f) {
                // 如果ViewModel设置了进度值，则使用该值
                uiState.progress
            } else {
                // 否则根据步骤完成情况计算进度
                when {
                    stepThreeCompleted -> 0.75f
                    stepTwoCompleted -> 0.5f
                    stepOneCompleted -> 0.25f
                    else -> 0f
                }
            },
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )

        // 自动滚动到下一步
        LaunchedEffect(stepOneCompleted, stepTwoCompleted, stepThreeCompleted) {
            when {
                stepOneCompleted && currentStep == 0 -> {
                    delay(300)
                    i18nVM.updateCurrentStep(1)
                    listState.animateScrollToItem(1)
                }

                stepTwoCompleted && currentStep == 1 -> {
                    delay(300)
                    i18nVM.updateCurrentStep(2)
                    listState.animateScrollToItem(2)
                }

                stepThreeCompleted && currentStep == 2 -> {
                    delay(300)
                    i18nVM.updateCurrentStep(3)
                    listState.animateScrollToItem(3)
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 项目信息和进度条
            if (project.isValid) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "当前项目地址： ${project.absolutePath}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.project.isValid) {
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { i18nVM.reset() },
                                    colors = filledTonalButtonColorsMd3()
                                ) {
                                    Text(
                                        "更换项目",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "完成进度",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.weight(1f).height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // 选项卡
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { i18nVM.updateSelectedTabIndex(0) },
                        text = { Text("配置") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { i18nVM.updateSelectedTabIndex(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("输出")
                                if (uiState.isExecuting) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (uiState.executionCompleted && uiState.commandOutput.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        painter = MaaIcons.Error,
                                        contentDescription = "完成",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                // 内容区域
                when (selectedTabIndex) {
                    0 -> {
                        // 配置页面
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            state = listState
                        ) {
                            // 步骤1: 输入i18n工具路径
                            item {
                                StepCard(
                                    stepNumber = 1,
                                    title = "设置I18N工具路径",
                                    isCompleted = stepOneCompleted,
                                    isExpanded = currentStep == 0,
                                    onClick = { i18nVM.updateCurrentStep(0) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        OutlinedTextField(
                                            value = uiState.toolPath,
                                            onValueChange = { i18nVM.updateToolPath(it) },
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            singleLine = true,
                                            colors = outlinedTextFieldColorsMd3(),
                                            placeholder = { Text("请输入I18N工具路径") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        FilledTonalButton(
                                            onClick = {
                                                if (uiState.toolPath.isNotBlank()) {
                                                    i18nVM.updateCurrentStep(1)
                                                }
                                            },
                                            enabled = uiState.toolPath.isNotBlank(),
                                            colors = filledTonalButtonColorsMd3(),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(
                                                "下一步",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 步骤2: 选择CSV文件
                            item {
                                StepCard(
                                    stepNumber = 2,
                                    title = "选择CSV文件",
                                    isCompleted = stepTwoCompleted,
                                    isExpanded = currentStep == 1,
                                    onClick = { i18nVM.updateCurrentStep(1) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        FileChooser(
                                            content = if (uiState.csvPath.isBlank()) "点击选择CSV文件" else "已选文件：${uiState.csvPath}",
                                            fileSelectionMode = JFileChooser.FILES_ONLY,
                                            fileFilter = FileNameExtensionFilter(
                                                "CSV文件",
                                                "csv"
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(100.dp)
                                        ) { i18nVM.importCsv(it) }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            if (currentStep > 0) {
                                                FilledTonalButton(
                                                    onClick = { i18nVM.updateCurrentStep(0) },
                                                    colors = filledTonalButtonColorsMd3()
                                                ) {
                                                    Text(
                                                        "上一步",
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            FilledTonalButton(
                                                onClick = {
                                                    if (uiState.csvPath.isNotBlank()) {
                                                        i18nVM.updateCurrentStep(2)
                                                    }
                                                },
                                                enabled = uiState.csvPath.isNotBlank(),
                                                colors = filledTonalButtonColorsMd3()
                                            ) {
                                                Text(
                                                    "下一步",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 步骤3: 选择目录
                            item {
                                StepCard(
                                    stepNumber = 3,
                                    title = "选择目标模块",
                                    isCompleted = stepThreeCompleted,
                                    isExpanded = currentStep == 2,
                                    onClick = { i18nVM.updateCurrentStep(2) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (uiState.modulePath.isNotBlank()) {
                                            Text(
                                                "已选择模块：${uiState.modulePath}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }

                                        Text(
                                            "可用模块列表：",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .verticalScroll(rememberScrollState())
                                                .padding(8.dp)
                                        ) {
                                            project.modules.forEach { module ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .onClick { i18nVM.selectModule(module.absolutePath) },
                                                    color = if (module.absolutePath == uiState.modulePath)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.surface,
                                                    shadowElevation = 1.dp
                                                ) {
                                                    Text(
                                                        module.name,
                                                        modifier = Modifier.padding(8.dp),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (module.absolutePath == uiState.modulePath)
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            if (currentStep > 0) {
                                                FilledTonalButton(
                                                    onClick = { i18nVM.updateCurrentStep(1) },
                                                    colors = filledTonalButtonColorsMd3()
                                                ) {
                                                    Text(
                                                        "上一步",
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            FilledTonalButton(
                                                onClick = {
                                                    if (uiState.modulePath.isNotBlank()) {
                                                        i18nVM.updateCurrentStep(3)
                                                    }
                                                },
                                                enabled = uiState.modulePath.isNotBlank(),
                                                colors = filledTonalButtonColorsMd3()
                                            ) {
                                                Text(
                                                    "下一步",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 步骤4: 执行操作
                            item {
                                StepCard(
                                    stepNumber = 4,
                                    title = "执行操作",
                                    isCompleted = uiState.executionCompleted,
                                    isExpanded = currentStep == 3,
                                    onClick = { i18nVM.updateCurrentStep(3) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Checkbox(
                                                checked = uiState.overrideMode,
                                                onCheckedChange = { i18nVM.setOverrideMode(it) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "字符串id冲突时覆盖原值",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    "转换前确认",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                InfoItem("工具路径", uiState.toolPath)
                                                InfoItem("CSV文件", uiState.csvPath)
                                                InfoItem("目标模块", uiState.modulePath)
                                                InfoItem(
                                                    "冲突处理",
                                                    if (uiState.overrideMode) "覆盖原值" else "保留原值"
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            if (currentStep > 0) {
                                                FilledTonalButton(
                                                    onClick = { i18nVM.updateCurrentStep(2) },
                                                    colors = filledTonalButtonColorsMd3()
                                                ) {
                                                    Text(
                                                        "上一步",
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            FilledTonalButton(
                                                onClick = {
                                                    i18nVM.translate()
                                                    // 执行后切换到输出选项卡由ViewModel处理
                                                },
                                                enabled = stepOneCompleted && stepTwoCompleted && stepThreeCompleted && !uiState.isExecuting,
                                                colors = filledTonalButtonColorsMd3()
                                            ) {
                                                if (uiState.isExecuting) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            "执行中...",
                                                            style = MaterialTheme.typography.labelLarge
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        if (uiState.executionCompleted) "重新执行" else "开始转换",
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // 输出页面
                        OutputConsole(
                            output = uiState.commandOutput,
                            onClear = { i18nVM.clearOutput() },
                            modifier = Modifier.fillMaxSize().weight(1f).padding(12.dp),
                        )
                    }
                }
            } else {
                FileChooser(
                    title = "添加项目地址",
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
                    modifier = Modifier.fillMaxSize(),
                    onFileSelected = i18nVM::importProject
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StepCard(
    stepNumber: Int,
    title: String,
    isCompleted: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 3.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isExpanded -> MaterialTheme.colorScheme.surface
                isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onClick { onClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 步骤数字指示器
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            painter = MaaIcons.Error,
                            contentDescription = "完成",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            "$stepNumber",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                if (!isExpanded) {
                    Icon(
                        painter = MaaIcons.Error,
                        contentDescription = "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 内容区域
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                content()
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

