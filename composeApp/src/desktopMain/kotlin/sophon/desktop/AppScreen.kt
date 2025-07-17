package sophon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import sophon.desktop.ui.components.AdbPath
import sophon.desktop.ui.components.ToolBar

class AppScreen : Screen {

    @Composable
    override fun Content() {
        val state by Context.stream.collectAsState()
        if (state.status is State.Status.Success) {
            Navigator(HomeScreen()) {
                Column(Modifier.fillMaxSize().background(Color.White)) {
                    ToolBar(
                        title = SlotManger.list.find { s -> s.second == it.lastItem.javaClass.name }?.first
                            ?: "首页",
                        isHome = !it.canPop,
                        onIconClick = it::pop
                    )
                    AdbPath(state, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Box(
                        modifier = Modifier.padding(
                            top = if (state.adbToolAvailable) 0.dp else 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                    ) {
                        CurrentScreen()
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