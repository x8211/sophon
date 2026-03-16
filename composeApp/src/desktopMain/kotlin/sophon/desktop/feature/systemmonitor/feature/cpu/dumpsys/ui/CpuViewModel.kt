package sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.systemmonitor.feature.cpu.common.domain.model.ThreadCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.data.repository.CpuRepositoryImpl
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.CpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.usecase.GetCpuDataUseCase

/**
 * CPU监测ViewModel
 */
class CpuViewModel : ViewModel() {

    private val repository = CpuRepositoryImpl()
    private val getCpuDataUseCase = GetCpuDataUseCase(repository)

    private val _uiState = MutableStateFlow<CpuUiState>(CpuUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // 当前选中的进程PID
    private val _selectedProcessPid = MutableStateFlow<Int?>(null)
    val selectedProcessPid = _selectedProcessPid.asStateFlow()
    
    // 进程线程列表
    private val _processThreads = MutableStateFlow<List<ThreadCpuInfo>>(emptyList())
    val processThreads = _processThreads.asStateFlow()
    
    // 线程加载状态
    private val _threadsLoading = MutableStateFlow(false)
    val threadsLoading = _threadsLoading.asStateFlow()
    
    // 线程数据最后更新时间戳
    private val _lastThreadUpdateTime = MutableStateFlow(0L)
    val lastThreadUpdateTime = _lastThreadUpdateTime.asStateFlow()
    
    // 持续监测任务
    private var monitoringJob: Job? = null
    
    // 监测刷新间隔（毫秒）
    private val monitoringIntervalMs = 2000L

    /**
     * 刷新CPU数据
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                // 如果是手动触发，不设置为 Loading，以避免界面闪烁
                val data = getCpuDataUseCase()
                _uiState.value = CpuUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = CpuUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 开始监测指定进程的线程信息
     * 会持续定时刷新线程数据，直到调用stopMonitoring()
     * @param pid 进程ID
     */
    fun startMonitoringProcessThreads(pid: Int) {
        // 如果已经在监测同一个进程，不需要重新启动
        if (_selectedProcessPid.value == pid && monitoringJob?.isActive == true) {
            return
        }
        
        // 停止之前的监测任务
        stopMonitoring()
        
        // 设置选中的进程
        _selectedProcessPid.value = pid
        
        // 启动持续监测任务
        monitoringJob = viewModelScope.launch {
            // 立即加载一次（首次显示Loading）
            loadThreadsOnce(pid, isFirstLoad = true)
            
            // 持续定时刷新（后续不显示Loading）
            while (isActive) {
                delay(monitoringIntervalMs)
                loadThreadsOnce(pid, isFirstLoad = false)
            }
        }
    }
    
    /**
     * 停止监测线程
     */
    fun stopMonitoring() {
        // 取消监测任务
        monitoringJob?.cancel()
        monitoringJob = null
        
        // 清空状态
        _selectedProcessPid.value = null
        _processThreads.value = emptyList()
        _lastThreadUpdateTime.value = 0L
    }
    
    /**
     * 加载一次线程信息（内部方法）
     * @param pid 进程ID
     * @param isFirstLoad 是否是首次加载，首次加载会显示Loading状态
     */
    private suspend fun loadThreadsOnce(pid: Int, isFirstLoad: Boolean) {
        try {
            // 只在首次加载时显示Loading状态
            if (isFirstLoad) {
                _threadsLoading.value = true
            }
            
            val threads = repository.getProcessThreads(pid)
            _processThreads.value = threads
            _lastThreadUpdateTime.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
            _processThreads.value = emptyList()
        } finally {
            // 只在首次加载时需要关闭Loading状态
            if (isFirstLoad) {
                _threadsLoading.value = false
            }
        }
    }
    
}


/**
 * CPU监测UI状态
 */
sealed class CpuUiState {
    data object Loading : CpuUiState()
    data class Success(val data: CpuData) : CpuUiState()
    data class Error(val message: String) : CpuUiState()
}
