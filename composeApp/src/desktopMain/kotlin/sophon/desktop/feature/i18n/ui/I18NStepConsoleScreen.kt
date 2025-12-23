package sophon.desktop.feature.i18n.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sophon.desktop.ui.components.OutputConsole

@Composable
fun I18NStepConsoleScreen(uiState: UiState, onClear: () -> Unit, modifier: Modifier) {
    OutputConsole(uiState.commandOutput, modifier, onClear)
}
