package sophon.desktop.feature.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import sophon.desktop.feature.apps.AppInfo

class InstalledAppsViewModel : ScreenModel {
    private val repository = InstalledAppsRepository()

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0f)
        private set

    var totalApps by mutableStateOf(0)
        private set

    var currentApp by mutableStateOf<AppInfo?>(null)
        private set

    fun loadApps() {
        screenModelScope.launch {
            isLoading = true
            progress = 0f
            apps = repository.getInstalledApps { current, total ->
                totalApps = total
                progress = current.toFloat() / total
            }
            isLoading = false
            
            // 默认选择第一个应用显示详情
            if (apps.isNotEmpty()) {
                selectApp(apps.first())
            }
        }
    }

    fun selectApp(app: AppInfo) {
        currentApp = app
    }
}