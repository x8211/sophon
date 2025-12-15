package sophon.desktop.ui

import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.StateFlow

interface LoggableScreen : Screen {
    val logOutput: StateFlow<String>
    fun onLogClear() {}
}
