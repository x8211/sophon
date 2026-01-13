package sophon.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import sophon.desktop.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        Surface(tonalElevation = 5.dp) {
            SophonApp()
        }
    }

}

fun main() = application {
    Window(
        onCloseRequest = {
            exitApplication()
        },
        title = "Sophon UI"
    ) {
        App()
    }
}
