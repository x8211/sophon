package sophon.desktop.feature.i18n.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import sophon.desktop.ui.components.DefaultListItem
import sophon.desktop.ui.components.FileChooser
import sophon.desktop.ui.theme.Dimens
import sophon.desktop.ui.theme.filledTonalButtonColorsMd3
import javax.swing.JFileChooser

enum class I18NScreen(val title: String) {
    Step1("选择CSV文件"),
    Step2("选择模块"),
    StepConsole("控制台输出"),
}

/**
 * 多语言页面
 */
@Composable
fun I18NScreen(
    navController: NavHostController = rememberNavController(),
    viewmodel: I18NViewModel = viewModel { I18NViewModel() }
) {
    val uiState by viewmodel.uiState.collectAsState()
    val project = uiState.project

    if (uiState.isLoading) {
        CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
        return
    }

    if (!project.isValid) {
        FileChooser(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            modifier = Modifier.fillMaxSize(),
            onFileSelected = viewmodel::importProject
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DefaultListItem(
            "当前项目",
            project.absolutePath,
            modifier = Modifier.background(Color(0xFFE3F2FD)).fillMaxWidth()
        ) {
            FilledTonalButton(
                onClick = { viewmodel.reset() },
                colors = filledTonalButtonColorsMd3()
            ) {
                Text(
                    "更换项目",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Column(modifier = Modifier.padding(Dimens.paddingMedium)) {
            Spacer(modifier = Modifier.height(Dimens.spacerSmall))
            InfoItem(
                label = "CSV文件",
                value = uiState.csvPath,
                isValid = uiState.csvPath.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(I18NScreen.Step1.name) }
            )
            Spacer(modifier = Modifier.height(Dimens.spacerSmall))
            InfoItem(
                label = "目标模块",
                value = uiState.modulePath,
                isValid = uiState.modulePath.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(I18NScreen.Step2.name) }
            )
            Spacer(modifier = Modifier.height(Dimens.spacerSmall))
            CheckedInfoItem(
                label = "字符串id冲突时覆盖原值",
                checked = uiState.overrideMode,
                modifier = Modifier.fillMaxWidth(),
                onCheckedChange = { viewmodel.setOverrideMode(it) }
            )
            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            val allStepsValid = uiState.toolPath.isNotEmpty() &&
                    uiState.csvPath.isNotEmpty() &&
                    uiState.modulePath.isNotEmpty()

            Button(
                onClick = {
                    viewmodel.translate()
                    navController.navigate(I18NScreen.StepConsole.name)
                },
                enabled = allStepsValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "开始转换",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("开始转换")
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(Dimens.paddingMedium)) {
            NavHost(
                navController = navController,
                startDestination = I18NScreen.Step1.name,
            ) {
                composable(route = I18NScreen.Step1.name) {
                    I18NStep2Screen(
                        onFileSelected = { viewmodel.importCsv(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(route = I18NScreen.Step2.name) {
                    I18NStep3Screen(
                        project,
                        uiState,
                        modifier = Modifier.fillMaxSize(),
                        onSelectModule = { viewmodel.selectModule(it) }
                    )
                }
                composable(route = I18NScreen.StepConsole.name) {
                    I18NStepConsoleScreen(
                        uiState,
                        modifier = Modifier.fillMaxSize(),
                        onClear = { viewmodel.clearOutput() }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isValid: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isValid) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    }
    val borderColor = if (isValid) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    }
    val icon = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning
    val iconColor =
        if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.paddingMedium, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(Dimens.spacerMedium))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(label)
                        append(": ")
                    }
                    if (value.isNotEmpty()) {
                        append(value)
                    } else {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("点击设置")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Suppress("SameParameterValue")
@Composable
private fun CheckedInfoItem(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.paddingMedium, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(Dimens.spacerMedium))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
