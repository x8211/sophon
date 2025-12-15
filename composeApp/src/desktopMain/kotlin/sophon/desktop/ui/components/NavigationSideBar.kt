package sophon.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun NavigationSideBar(
    items: List<Pair<String, String>>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().animateContentSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Toggle Button
            IconButton(
                onClick = { onExpandChange(!isExpanded) },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                    contentDescription = "Toggle Navigation"
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp), // Bottom padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items.forEach { (title, className) ->
                            val icon = remember(title) { getIconForTitle(title) }
                            val selected = currentRoute == className

                            NavigationRailItem(
                                selected = selected,
                                onClick = { onNavigate(className) },
                                icon = {
                                    Icon(icon, contentDescription = title)
                                },
                                label = {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getIconForTitle(title: String): ImageVector {
    return when {
        title.contains("首页", true) -> Icons.Default.Home
        title.contains("Home", true) -> Icons.Default.Home
        title.contains("APK", true) -> Icons.Default.Android
        title.contains("Log", true) -> Icons.Default.BugReport
        title.contains("I18N", true) -> Icons.Default.Language
        title.contains("DeepLink", true) -> Icons.Default.Link
        title.contains("Proxy", true) -> Icons.Default.Build
        title.contains("Task", true) -> Icons.Default.Dashboard
        title.contains("Shell", true) -> Icons.Default.Code
        else -> Icons.Default.Extension
    }
}
