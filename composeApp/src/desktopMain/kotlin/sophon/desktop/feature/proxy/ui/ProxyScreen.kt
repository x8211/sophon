package sophon.desktop.feature.proxy.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sophon.desktop.feature.proxy.model.LocalNetworkInterface
import sophon.desktop.ui.components.SectionTitle
import sophon.desktop.ui.components.SwitchListItem

private val HeaderBg = Color(0xFFF5F5F5)
private val DividerColor = Color(0xFFDDDDDD)
private val InterfaceColumnWeight = 0.45f

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

        IpAddressTable(
            items = proxyInfo.options,
            onItemClick = { viewModel.setProxy(it.ipAddress) }
        )
    }
}

@Composable
private fun IpAddressTable(
    items: List<LocalNetworkInterface>,
    modifier: Modifier = Modifier,
    onItemClick: (LocalNetworkInterface) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 表头
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "网络接口",
                modifier = Modifier.weight(InterfaceColumnWeight),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF555555)
            )
            Text(
                text = "IP 地址",
                modifier = Modifier.weight(1f - InterfaceColumnWeight),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF555555)
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        // 数据行
        LazyColumn(verticalArrangement = Arrangement.Center) {
            itemsIndexed(items) { index, item ->
                IpAddressRow(
                    item = item,
                    onClick = { onItemClick(item) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = DividerColor
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

@Composable
private fun IpAddressRow(
    item: LocalNetworkInterface,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, indication = null, interactionSource = null)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.name,
            modifier = Modifier.weight(InterfaceColumnWeight),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = item.ipAddress,
            modifier = Modifier.weight(1f - InterfaceColumnWeight),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1A1A1A)
        )
    }
}
