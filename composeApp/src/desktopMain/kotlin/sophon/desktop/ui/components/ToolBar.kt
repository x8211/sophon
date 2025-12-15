package sophon.desktop.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolBar(title: String, isHome: Boolean = false, onIconClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        navigationIcon = {
            if (isHome) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "主页",
                        modifier = Modifier.size(Dimens.iconSizeMedium)
                    )
                }
            } else {
                IconButton(onClick = onIconClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "返回",
                        modifier = Modifier.size(Dimens.iconSizeMedium)
                    )
                }
            }
        },
        windowInsets = WindowInsets(0.dp)
    )
}