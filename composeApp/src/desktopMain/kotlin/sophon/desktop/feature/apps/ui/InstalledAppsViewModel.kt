package sophon.desktop.feature.apps.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import sophon.desktop.feature.apps.data.repository.InstalledAppsRepositoryImpl
import sophon.desktop.feature.apps.domain.model.AppInfo
import sophon.desktop.feature.apps.domain.model.AppLoadState
import sophon.desktop.feature.apps.domain.usecase.GetInstalledAppsUseCase

class InstalledAppsViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase = GetInstalledAppsUseCase(InstalledAppsRepositoryImpl())
) : ViewModel() {

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0f)
        private set

    var totalApps by mutableStateOf(0)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var currentApp by mutableStateOf<AppInfo?>(null)
        private set

    fun loadApps() {
        viewModelScope.launch {
            getInstalledAppsUseCase().collect { state ->
                when (state) {
                    is AppLoadState.Idle -> {
                        isLoading = false
                        progress = 0f
                    }
                    is AppLoadState.Loading -> {
                        isLoading = true
                        progress = 0f
                        errorMessage = null
                        apps = emptyList()
                    }
                    is AppLoadState.Progress -> {
                        isLoading = true
                        totalApps = state.total
                        progress = if (state.total > 0) state.current.toFloat() / state.total else 0f
                    }
                    is AppLoadState.Success -> {
                        isLoading = false
                        progress = 1f
                        apps = state.apps
                        // 默认选择第一个应用显示详情
                        if (apps.isNotEmpty()) {
                            selectApp(apps.first())
                        }
                    }
                    is AppLoadState.Error -> {
                        isLoading = false
                        errorMessage = state.message
                    }
                }
            }
        }
    }

    fun selectApp(app: AppInfo) {
        currentApp = app
    }
}
