package sophon.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.processor.SlotManger
import sophon.desktop.core.Context
import sophon.desktop.ui.components.FileChooser

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adbUiState by Context.stream.collectAsState()

        val slots = remember {
            SlotManger.list.map {
                it.first to Class.forName(it.second).getConstructor().newInstance() as Screen
            }
        }

        if (adbUiState.adbToolAvailable) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(slots.size) {
                    SlotItem(
                        title = slots[it].first,
                        screen = slots[it].second
                    ) { screen -> navigator.push(screen) }
                }
            }
        } else {
            FileChooser(title = "添加Adb可执行文件", modifier = Modifier.fillMaxSize()) {
                Context.importAdb(it)
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SlotItem(
        title: String,
        screen: Screen,
        onItemClick: (Screen) -> Unit,
    ) {
        Card(
            modifier = Modifier.aspectRatio(1f),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            onClick = { onItemClick.invoke(screen) }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}