package sophon.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.Navigator
import sophon.desktop.core.SophonSocketRepository
import sophon.desktop.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    Navigator(AppScreen())
}

fun main() = application {
    Window(
        onCloseRequest = {
//            SophonSocketRepository.disconnect()
            exitApplication()
        },
        title = "Sophon UI"
    ) {
        AppTheme {
            Surface(tonalElevation = 5.dp) {
                App()
            }
        }
    }
}
