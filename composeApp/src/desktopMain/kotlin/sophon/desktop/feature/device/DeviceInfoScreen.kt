package sophon.desktop.feature.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import sophon.desktop.processor.annotation.Slot

@Slot("设备信息")
class DeviceInfoScreen : Screen {

    @Composable
    override fun Content() {
        val deviceVM = rememberScreenModel { DeviceInfoViewModel() }
        val deviceState by deviceVM.state.collectAsState()
        val sections = deviceState.sections
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .simpleVerticalScrollbar(listState)
        ) {
            items(sections) { section ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = section.first,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))

                        section.second.forEach { (key, value) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Magenta,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}