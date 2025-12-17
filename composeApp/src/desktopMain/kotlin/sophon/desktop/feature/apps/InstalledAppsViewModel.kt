package sophon.desktop.feature.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class InstalledAppsViewModel : ViewModel() {
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
        viewModelScope.launch {
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