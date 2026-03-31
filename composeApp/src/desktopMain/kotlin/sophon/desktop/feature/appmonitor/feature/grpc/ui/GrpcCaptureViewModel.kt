package sophon.desktop.feature.appmonitor.feature.grpc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sophon.desktop.feature.appmonitor.feature.grpc.data.repository.GrpcCaptureRepositoryImpl
import sophon.desktop.feature.appmonitor.feature.grpc.domain.model.GrpcCaptureModel
import sophon.desktop.feature.appmonitor.feature.grpc.domain.usecase.GetGrpcCaptureUseCase
import sophon.desktop.feature.appmonitor.feature.grpc.domain.usecase.RefreshGrpcCaptureUseCase

/**
 * gRPC 捕获功能的 ViewModel
 *
 * 使用 2 秒间隔轮询持续从设备拉取 Protodroid.db 并刷新记录列表。
 * 轮询模式参考 SystemMonitorViewModel：通过 Job 控制启停，采用
 * `while (isActive)` + `delay` 实现可取消的循环。
 */
class GrpcCaptureViewModel : ViewModel() {

    private val repository = GrpcCaptureRepositoryImpl()
    private val getGrpcCaptureUseCase = GetGrpcCaptureUseCase(repository)
    private val refreshGrpcCaptureUseCase = RefreshGrpcCaptureUseCase(repository)

    private val _uiState = MutableStateFlow<GrpcCaptureUiState>(GrpcCaptureUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /** 轮询任务 */
    private var pollingJob: Job? = null

    /** 轮询间隔（毫秒） */
    companion object {
        private const val POLLING_INTERVAL_MS = 2000L
    }

    /**
     * 启动轮询
     *
     * 每 2 秒执行一次：run-as 拉取数据库 → 读取记录 → 更新 UI 状态。
     * 首次执行时显示 Loading 状态，后续轮询静默更新（避免闪烁）。
     *
     * @param packageName 当前前台应用的包名，由 AppMonitorScreen 传入
     */
    fun startPolling(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            _uiState.value = GrpcCaptureUiState.Error("包名为空，请等待应用信息加载完成")
            return
        }

        // 取消之前的轮询任务
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            // 首次显示 Loading
            val isFirstRun = _uiState.value is GrpcCaptureUiState.Idle
            if (isFirstRun) {
                _uiState.value = GrpcCaptureUiState.Loading
            }
            println("[GrpcCaptureVM] 启动轮询，包名: $packageName，间隔: ${POLLING_INTERVAL_MS}ms")

            while (isActive) {
                try {
                    val success = refreshGrpcCaptureUseCase(packageName)
                    if (success) {
                        val records = getGrpcCaptureUseCase()
                        println("[GrpcCaptureVM] 轮询刷新成功，读取到 ${records.size} 条记录")
                        _uiState.value = GrpcCaptureUiState.Success(records)
                    } else {
                        // 拉取失败 → 设置错误状态并停止轮询
                        println("[GrpcCaptureVM] 拉取数据库失败，停止轮询，包名: $packageName")
                        _uiState.value = GrpcCaptureUiState.Error(
                            "无法拉取 $packageName 的数据库。\n请确保：\n1. 设备已连接\n2. 应用为 debuggable 版本\n3. 已安装 Protodroid"
                        )
                        break
                    }
                } catch (e: Exception) {
                    // 异常 → 设置错误状态并停止轮询
                    println("[GrpcCaptureVM] 轮询异常，停止轮询: ${e.message}")
                    _uiState.value = GrpcCaptureUiState.Error("轮询失败: ${e.message}")
                    break
                }

                // 每 2 秒轮询一次（使用可取消的 delay）
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    /**
     * 停止轮询
     */
    fun stopPolling() {
        println("[GrpcCaptureVM] 停止轮询")
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * 手动立即刷新一次（重置状态，重新启动轮询）
     *
     * @param packageName 当前前台应用的包名
     */
    fun refreshData(packageName: String?) {
        _uiState.value = GrpcCaptureUiState.Loading
        startPolling(packageName)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

/**
 * gRPC 捕获页面 UI 状态
 */
sealed class GrpcCaptureUiState {
    /** 初始状态，尚未执行任何操作 */
    object Idle : GrpcCaptureUiState()

    /** 正在拉取和读取数据 */
    object Loading : GrpcCaptureUiState()

    /**
     * 成功读取数据
     *
     * @param records 读取到的记录列表
     */
    data class Success(val records: List<GrpcCaptureModel>) : GrpcCaptureUiState()

    /**
     * 发生错误
     *
     * @param message 错误描述信息
     */
    data class Error(val message: String) : GrpcCaptureUiState()
}
