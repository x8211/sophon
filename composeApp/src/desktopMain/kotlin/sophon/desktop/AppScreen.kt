package sophon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.datastore.featureUsageDataStore
import sophon.desktop.feature.activitystack.ActivityStackScreen
import sophon.desktop.feature.apps.InstalledAppsScreen
import sophon.desktop.feature.deeplink.DeepLinkScreen
import sophon.desktop.feature.developer.DeveloperScreen
import sophon.desktop.feature.device.DeviceInfoScreen
import sophon.desktop.feature.i18n.I18NScreen
import sophon.desktop.feature.installapk.InstallApkScreen
import sophon.desktop.feature.proxy.ProxyScreen
import sophon.desktop.feature.screen.ScreenInfoScreen
import sophon.desktop.feature.thread.ThreadScreen
import sophon.desktop.ui.components.AdbPath
import sophon.desktop.ui.components.NavigationSideBar
import sophon.desktop.ui.components.ToolBar
import sophon.desktop.ui.theme.Dimens

enum class AppScreen(val title: String) {
    Home("首页"),
    ActivityStack("Activity栈"),
    Proxy("设置代理"),
    Developer("开发者选项"),
    Deeplink("Deeplink"),
    InstallApk("安装Apk"),
    InstalledApps("已安装应用"),
    ThreadInfo("线程信息"),
    I18N("多语言"),
    ScreenInfo("屏幕信息"),
}

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
        list.remove(AppScreen.Home)
        list.sortByDescending { usage.usages[it.name] ?: 0 }
        sortedItems = listOf(AppScreen.Home) + list
    }

    Row(Modifier.fillMaxSize().background(Color.White)) {
        // Left Side: Function Entry
        NavigationSideBar(
            items = sortedItems,
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
        Column(Modifier.weight(1f)) {
            // Top Layout: Title and AdbPath
            Column(Modifier.fillMaxWidth()) {
                ToolBar(title = currentScreen.title)
                AdbPath(
                    state,
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.paddingMedium)
                )
            }

            // Middle: Main Feature Area
            Box(modifier = Modifier.weight(1f)) {
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
                    composable(route = AppScreen.InstalledApps.name) { InstalledAppsScreen() }
                    composable(route = AppScreen.I18N.name) { I18NScreen() }
                    composable(route = AppScreen.ThreadInfo.name) { ThreadScreen() }
                    composable(route = AppScreen.ScreenInfo.name) { ScreenInfoScreen() }
                }
            }
        }
    }
}