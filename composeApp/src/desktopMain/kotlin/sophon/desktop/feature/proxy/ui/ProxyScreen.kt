package sophon.desktop.feature.proxy.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.ui.components.DefaultListItem
import sophon.desktop.ui.components.SectionTitle
import sophon.desktop.ui.components.SwitchListItem

/**
 * 设置手机代理
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProxyScreen(viewModel: ProxyViewModel = viewModel { ProxyViewModel() }) {
    val proxyInfo by viewModel.uiState.collectAsState()

    Column {
        SwitchListItem(
            "当前代理：${proxyInfo.current}",
            modifier = Modifier.fillMaxWidth(),
            checked = proxyInfo.proxyEnabled,
            onCheckedChange = { if (!it) viewModel.resetProxy() }
        )

        SectionTitle("本机IP地址", modifier = Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.Center) {
            items(proxyInfo.options.count()) {
                DefaultListItem(
                    proxyInfo.options[it],
                    onClick = { viewModel.setProxy(proxyInfo.options[it]) })

                if (it < proxyInfo.options.count() - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = Color(0xFFEEEEEE)
                    )
                }
            }
        }
    }
}
