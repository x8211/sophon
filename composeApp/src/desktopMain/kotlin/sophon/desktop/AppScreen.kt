package sophon.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.core.State
import sophon.desktop.core.usage.featureUsageDataStore
import sophon.desktop.feature.activitystack.ui.ActivityStackScreen
import sophon.desktop.feature.cpumonitor.ui.CpuMonitorScreen
import sophon.desktop.feature.deeplink.ui.DeepLinkScreen
import sophon.desktop.feature.developer.ui.DeveloperScreen
import sophon.desktop.feature.device.ui.DeviceInfoScreen
import sophon.desktop.feature.gfxmonitor.ui.GfxMonitorScreen
import sophon.desktop.feature.i18n.ui.I18NScreen
import sophon.desktop.feature.installapk.ui.InstallApkScreen
import sophon.desktop.feature.proxy.ui.ProxyScreen
import sophon.desktop.feature.settings.ui.SettingsScreen
import sophon.desktop.feature.systemmonitor.ui.SystemMonitorScreen
import sophon.desktop.feature.thread.ui.ThreadScreen
import sophon.desktop.ui.theme.Dimens
import sophon.desktop.ui.theme.inputChipColorsMd3
import sophon.desktop.ui.theme.menuItemColorsMd3

enum class AppScreen(val title: String) {
    Home("首页"),
    ActivityStack("Activity栈"),
    Proxy("设置代理"),
    Developer("开发者选项"),
    Deeplink("Deeplink"),
    InstallApk("安装Apk"),
    ThreadInfo("线程信息"),
    SystemMonitor("系统监测"),
    GfxMonitor("图形监测"),
    CpuMonitor("CPU监测"),
    I18N("多语言"),
    Settings("设置"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SophonApp(navController: NavHostController = rememberNavController()) {
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = AppScreen.valueOf(
        backStackEntry?.destination?.route ?: AppScreen.Home.name
    )
    var isExpanded by remember { mutableStateOf(true) }
    val state by Context.stream.collectAsState()
    val scope = rememberCoroutineScope()
    var sortedItems by remember { mutableStateOf(AppScreen.entries.toList()) }

    LaunchedEffect(Unit) {
        val usage = featureUsageDataStore.data.first()
        val list = AppScreen.entries.toMutableList()
        list.sortByDescending { usage.usages[it.name] ?: 0 }
        sortedItems = list
    }

    Row(Modifier.fillMaxSize()) {
        // Left Side: Function Entry
        NavigationSideBar(
            items = sortedItems,
            fixedHeader = AppScreen.Home,
            fixedFooter = AppScreen.Settings,
            currentScreen = currentScreen,
            onNavigate = {
                navController.navigate(it.name)
                scope.launch {
                    featureUsageDataStore.updateData { current ->
                        val newCount = (current.usages[it.name] ?: 0) + 1
                        current.copy(usages = current.usages + (it.name to newCount))
                    }
                }
            },
            isExpanded = isExpanded,
            onExpandChange = { isExpanded = it }
        )

        // Right Side: Main Content
        Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surface)) {
            // Top Layout: Title
            TopAppBar(
                title = {
                    Text(currentScreen.title, style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    if (state.adbToolAvailable) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Dimens.paddingSmall)
                        ) {
                            Text("已连接设备：", style = MaterialTheme.typography.titleSmall)
                            AttachedDeviceDropdownMenu(state) { Context.selectDevice(it) }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
            )

            // Middle: Main Feature Area
            Box(modifier = Modifier.weight(1f).background(Color.White)) {
                NavHost(
                    navController = navController,
                    startDestination = AppScreen.Home.name,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(route = AppScreen.Home.name) { DeviceInfoScreen() }
                    composable(route = AppScreen.ActivityStack.name) { ActivityStackScreen() }
                    composable(route = AppScreen.Proxy.name) { ProxyScreen() }
                    composable(route = AppScreen.Developer.name) { DeveloperScreen() }
                    composable(route = AppScreen.Deeplink.name) { DeepLinkScreen() }
                    composable(route = AppScreen.InstallApk.name) { InstallApkScreen() }
                    composable(route = AppScreen.I18N.name) { I18NScreen() }
                    composable(route = AppScreen.ThreadInfo.name) { ThreadScreen() }
                    composable(route = AppScreen.SystemMonitor.name) { SystemMonitorScreen() }
                    composable(route = AppScreen.GfxMonitor.name) { GfxMonitorScreen() }
                    composable(route = AppScreen.CpuMonitor.name) { CpuMonitorScreen() }
                    composable(route = AppScreen.Settings.name) { SettingsScreen() }
                }
            }
        }
    }
}

/**
 * 已关联设备列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachedDeviceDropdownMenu(state: State, onSelectDevice: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        InputChip(
            onClick = { expanded = true },
            selected = false,
            shape = MaterialTheme.shapes.small,
            colors = inputChipColorsMd3(),
            label = {
                Text(
                    text = state.selectedDevice,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    "展开收起按钮",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.connectingDevices.forEach {
                DropdownMenuItem(text = { Text(it) }, colors = menuItemColorsMd3(), onClick = {
                    expanded = false
                    onSelectDevice.invoke(it)
                })
            }
        }
    }
}

@Composable
fun NavigationSideBar(
    items: List<AppScreen>,
    fixedHeader: AppScreen?,
    fixedFooter: AppScreen?,
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
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
                    if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                    contentDescription = "Toggle Navigation"
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacerSmall),
                    modifier = Modifier.padding(bottom = Dimens.spacerSmall)
                ) {
                    fixedHeader?.let {
                        SidebarItem(
                            screen = it,
                            isSelected = currentScreen == it,
                            onClick = onNavigate,
                        )
                    }

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacerSmall)
                    ) {
                        items.filterNot { it == fixedHeader || it == fixedFooter }.forEach {
                            SidebarItem(it, currentScreen == it, onNavigate)
                        }
                    }

                    fixedFooter?.let {
                        SidebarItem(
                            screen = it,
                            isSelected = currentScreen == it,
                            onClick = onNavigate,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 侧边栏条目组件
 */
@Composable
private fun SidebarItem(
    screen: AppScreen,
    isSelected: Boolean,
    onClick: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = remember(screen.title) { getIconForTitle(screen.title) }
    NavigationRailItem(
        selected = isSelected,
        onClick = { onClick(screen) },
        icon = { Icon(icon, contentDescription = screen.title) },
        label = {
            Text(
                screen.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false
            )
        },
        alwaysShowLabel = true,
        modifier = modifier
    )
}

private fun getIconForTitle(title: String): ImageVector {
    return when (title) {
        AppScreen.Home.title -> Icons.Default.Home
        AppScreen.InstallApk.title -> Icons.Default.Android
        AppScreen.I18N.title -> Icons.Default.Language
        AppScreen.Deeplink.title -> Icons.Default.Link
        AppScreen.Proxy.title -> Icons.Default.Build
        AppScreen.ActivityStack.title -> Icons.Default.Dashboard
        AppScreen.Settings.title -> Icons.Default.Settings
        AppScreen.Developer.title -> Icons.Default.DeveloperMode
        AppScreen.SystemMonitor.title -> Icons.Default.Monitor
        AppScreen.GfxMonitor.title -> Icons.Default.GraphicEq
        AppScreen.CpuMonitor.title -> Icons.Default.Speed
        else -> Icons.Default.Extension
    }
}