package sophon.desktop.feature.i18n.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sophon.desktop.ui.theme.outlinedTextFieldColorsMd3

@Composable
fun I18NStep1Screen(
    uiState: UiState,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = uiState.toolPath,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        colors = outlinedTextFieldColorsMd3(),
        placeholder = { Text("请输入I18N工具路径") },
        modifier = modifier
    )
}
