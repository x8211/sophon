package sophon.desktop.feature.appmonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.appmonitor.data.repository.AppMonitorRepositoryImpl
import sophon.desktop.feature.appmonitor.domain.model.AppInfo
import sophon.desktop.feature.appmonitor.domain.usecase.GetForegroundAppInfoUseCase

/**
 * 应用监控ViewModel
 *
 * 管理应用监控页面的状态，包括当前应用信息和子功能选择
 * 使用2秒轮询机制持续获取前台应用包名和信息
 */
class AppMonitorViewModel : ViewModel() {

    private val repository = AppMonitorRepositoryImpl()
    private val getForegroundAppInfoUseCase = GetForegroundAppInfoUseCase(repository)

    // 当前应用信息状态
    private val _appInfo = MutableStateFlow<AppInfo?>(null)
    val appInfo = _appInfo.asStateFlow()

    // 当前选中的子功能
    private val _selectedFeature = MutableStateFlow(AppMonitorFeature.THREAD)
    val selectedFeature = _selectedFeature.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // 刷新触发器，每次轮询后递增，用于触发子功能刷新
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    // 轮询任务
    private var pollingJob: Job? = null

    /**
     * 启动轮询获取前台应用信息
     * 每2秒轮询一次
     */
    fun startPolling() {
        // 取消之前的轮询任务
        pollingJob?.cancel()

        // 启动新的轮询任务
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _errorMessage.value = null

                    val info = getForegroundAppInfoUseCase()
                    if (info != null) {
                        // 每次都更新，确保子功能能及时获取最新包名
                        _appInfo.value = info
                        // 递增刷新触发器，通知子功能刷新
                        _refreshTrigger.value += 1
                    } else {
                        _errorMessage.value =
                            "无法获取前台应用信息，请确保设备已连接且有应用在前台运行"
                        _appInfo.value = null
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "加载失败: ${e.message}"
                    _appInfo.value = null
                }

                // 每2秒轮询一次（使用可取消的delay）
                delay(2000)
            }
        }
    }

    /**
     * 停止轮询
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _appInfo.value = null
    }

    /**
     * 选择子功能
     *
     * @param feature 要选择的子功能
     */
    fun selectFeature(feature: AppMonitorFeature) {
        _selectedFeature.value = feature
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

/**
 * 应用监控子功能枚举
 */
enum class AppMonitorFeature(val displayName: String) {
    THREAD("线程信息"),
    FILE_EXPLORER("文件浏览器"),
    ACTIVITY_STACK("Activity栈")
}
