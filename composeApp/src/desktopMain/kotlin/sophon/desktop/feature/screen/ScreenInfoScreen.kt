package sophon.desktop.feature.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot

@Slot("屏幕信息")
class ScreenInfoScreen : Screen {

    @Composable
    override fun Content() {
        val screenVM = rememberScreenModel { ScreenInfoViewModel() }
        val uiState by screenVM.uiState.collectAsState()

        Column(modifier = Modifier.padding(6.dp)) {
            InfoCard(title = "屏幕分辨率") {
                // 物理分辨率
                InfoItemContainer(title = "物理分辨率") {
                    Text(
                        "${uiState.physicalWidth}x${uiState.physicalHeight}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 覆盖分辨率
                OverrideItem(
                    "覆盖分辨率",
                    if (uiState.overrideWidth.isNotBlank() && uiState.overrideHeight.isNotBlank()) "${uiState.overrideWidth}x${uiState.overrideHeight}"
                    else "unknown",
                    uiState.overrideWidth.isNotBlank() && uiState.overrideHeight.isNotBlank()
                ) {
                    screenVM.resetResolution()
                }

                // 修改区域
                ResolutionInputItem(
                    width = uiState.inputWidth,
                    height = uiState.inputHeight,
                    onModifyWidthInput = { screenVM.modifyWidthInput(it) },
                    onModifyHeightInput = { screenVM.modifyHeightInput(it) },
                    onModifyResolution = { screenVM.modifyResolution() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            InfoCard(title = "屏幕密度") {
                // 物理密度
                InfoItemContainer(title = "物理屏幕密度") {
                    Text(
                        uiState.physicalDensity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 覆盖密度
                OverrideItem(
                    "覆盖屏幕密度",
                    uiState.overrideDensity.ifBlank { "unknown" },
                    uiState.overrideDensity.isNotBlank()
                ) { screenVM.resetDensity() }

                // 修改区域
                DensityInputItem(
                    value = uiState.inputDensity,
                    onModifyInput = { screenVM.modifyDensityInput(it) },
                    onModify = { screenVM.modifyDensity() }
                )
            }
        }
    }

    @Composable
    private fun InfoCard(
        title: String,
        content: @Composable () -> Unit,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                content()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ResolutionInputItem(
        width: String,
        height: String,
        onModifyWidthInput: (String) -> Unit,
        onModifyHeightInput: (String) -> Unit,
        onModifyResolution: () -> Unit,
    ) {
        InfoItemContainer(title = "修改分辨率") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 宽度输入
                Column(modifier = Modifier.weight(1f)) {
                    LabeledTextField(
                        label = "宽",
                        value = width,
                        onValueChange = { onModifyWidthInput.invoke(it) }
                    )
                }

                Spacer(Modifier.width(3.dp))

                // 高度输入
                Column(modifier = Modifier.weight(1f)) {
                    LabeledTextField(
                        label = "高",
                        value = height,
                        onValueChange = { onModifyHeightInput.invoke(it) }
                    )
                }

                Spacer(Modifier.width(3.dp))

                // 修改按钮
                ActionButton(
                    text = "修改",
                    onClick = { onModifyResolution.invoke() }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DensityInputItem(
        value: String,
        onModifyInput: (String) -> Unit,
        onModify: () -> Unit,
    ) {
        InfoItemContainer(title = "修改密度") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactTextField(
                        value = value,
                        onValueChange = { onModifyInput.invoke(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(3.dp))

                ActionButton(
                    text = "修改",
                    onClick = { onModify.invoke() }
                )
            }
        }
    }

    @Composable
    private fun OverrideItem(
        title: String,
        value: String,
        showReset: Boolean,
        resetAction: () -> Unit,
    ) {
        InfoItemContainer(title = title) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (showReset) {
                    ActionButton(
                        text = "重置",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = resetAction
                    )
                }
            }
        }
    }

    @Composable
    private fun InfoItemContainer(
        title: String,
        content: @Composable () -> Unit,
    ) {
        Surface(
            modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                content()
            }
        }
    }

    @Composable
    private fun LabeledTextField(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            CompactTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun ActionButton(
        text: String,
        icon: ImageVector? = null,
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        onClick: () -> Unit,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor
            ),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }

    @Composable
    private fun CompactTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}