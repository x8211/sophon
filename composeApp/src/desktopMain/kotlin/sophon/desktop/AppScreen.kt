package sophon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.processor.SlotManger
import sophon.desktop.core.Context
import sophon.desktop.core.State
import sophon.desktop.feature.device.DeviceInfoScreen
import sophon.desktop.ui.components.AdbPath
import sophon.desktop.ui.components.NavigationSideBar
import sophon.desktop.ui.components.OutputConsole
import sophon.desktop.ui.components.ToolBar
import sophon.desktop.ui.theme.Dimens

class AppScreen : Screen {

    @Composable
    override fun Content() {
        val state by Context.stream.collectAsState()
        var isExpanded by remember { mutableStateOf(true) }

        if (state.status is State.Status.Success) {
            Navigator(DeviceInfoScreen()) { navigator ->
                Row(Modifier.fillMaxSize().background(Color.White)) {
                    // Left Side: Function Entry
                    val slots = remember {
                        listOf("首页" to DeviceInfoScreen::class.java.name) + SlotManger.list
                    }

                    NavigationSideBar(
                        items = slots,
                        currentRoute = navigator.lastItem::class.java.name,
                        onNavigate = { className ->
                            if (className == DeviceInfoScreen::class.java.name) {
                                navigator.replaceAll(DeviceInfoScreen())
                            } else {
                                try {
                                    val screenClass = Class.forName(className)
                                    val screenInstance =
                                        screenClass.getConstructor().newInstance() as Screen
                                    navigator.replaceAll(screenInstance)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        isExpanded = isExpanded,
                        onExpandChange = { isExpanded = it }
                    )

                    // Right Side: Main Content
                    Column(Modifier.weight(1f)) {
                        // Top Layout: Title and AdbPath
                        Column(Modifier.fillMaxWidth()) {
                            ToolBar(
                                title = slots.find { s -> s.second == navigator.lastItem.javaClass.name }?.first
                                    ?: "首页",
                                isHome = true, // Navigation is handled by SideBar
                                onIconClick = {}
                            )
                            AdbPath(
                                state,
                                Modifier.fillMaxWidth().padding(horizontal = Dimens.paddingMedium)
                            )
                        }

                        // Middle: Main Feature Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            CurrentScreen()
                        }

                        // Bottom: Output Console (if needed)
                        val currentScreen = navigator.lastItem
                        if (currentScreen is sophon.desktop.ui.LoggableScreen) {
                            val logs by currentScreen.logOutput.collectAsState()
                            OutputConsole(
                                output = logs,
                                onClear = currentScreen::onLogClear,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp) // Fixed height for console area
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.status.text)
            }
        }
    }
}