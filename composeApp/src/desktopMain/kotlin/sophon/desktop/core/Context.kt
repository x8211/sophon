package sophon.desktop.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sophon.desktop.feature.adb.data.repository.AdbRepositoryImpl
import sophon.desktop.feature.adb.domain.model.AdbStatus
import sophon.desktop.feature.adb.domain.usecase.AutoFindAdbUseCase
import sophon.desktop.feature.adb.domain.usecase.FormatAdbCommandUseCase
import sophon.desktop.feature.adb.domain.usecase.GetAdbStateUseCase
import sophon.desktop.feature.adb.domain.usecase.RefreshDevicesUseCase
import sophon.desktop.feature.adb.domain.usecase.SelectDeviceUseCase
import sophon.desktop.feature.adb.domain.usecase.UpdateAdbPathUseCase

/**
 * 全局核心状态单例，充当全局 ViewModel 的角色。
 * 遵循 Clean Architecture 改造，业务逻辑委托给 UseCases。
 */
object Context {

    private val scope = MainScope()

    // 初始化数据层与领域层组件
    private val adbRepository = AdbRepositoryImpl(scope)
    private val getAdbStateUseCase = GetAdbStateUseCase(adbRepository)
    private val updateAdbPathUseCase = UpdateAdbPathUseCase(adbRepository)
    private val selectDeviceUseCase = SelectDeviceUseCase(adbRepository)
    private val refreshDevicesUseCase = RefreshDevicesUseCase(adbRepository)
    private val autoFindAdbUseCase = AutoFindAdbUseCase(adbRepository)
    private val formatAdbCommandUseCase = FormatAdbCommandUseCase()

    // 暴露给外界的状态流，映射领域模型到 UI State 以保持 API 兼容性
    val stream: StateFlow<State> = getAdbStateUseCase()
        .map { adbState ->
            State(
                status = adbState.status.toUiStatus(),
                adbToolPath = adbState.adbToolPath,
                adbToolAvailable = adbState.adbToolAvailable,
                connectingDevices = adbState.connectingDevices,
                selectedDevice = adbState.selectedDevice,
                adbParentPath = adbState.adbParentPath
            )
        }.stateIn(scope, SharingStarted.Eagerly, State())

    // 兼容性字段
    val adbParentPath: String?
        get() = stream.value.adbParentPath

    init {
        // 定时轮询设备
        scope.launch {
            while (true) {
                refreshDevicesUseCase()
                delay(3000)
            }
        }

        // 初始检查与自动寻找
        scope.launch {
            val currentState = getAdbStateUseCase().first()
            if (!currentState.adbToolAvailable) {
                val foundPath = autoFindAdbUseCase()
                if (foundPath != null) {
                    updateAdbPathUseCase(foundPath)
                }
            }
        }
    }

    /**
     * 选择目标设备
     */
    fun selectDevice(deviceName: String) {
        scope.launch {
            selectDeviceUseCase(deviceName)
        }
    }

    /**
     * 格式化 ADB 命令。
     * 调用 FormatAdbCommandUseCase，底层会根据当前选中的设备及其路径进行拼装。
     */
    fun formatIfAdbCmd(input: String): String {
        val currentState = adbRepository.getAdbState().value
        return formatAdbCommandUseCase(input, currentState)
    }

    // --- 状态数据模型适配层 ---

    private fun AdbStatus.toUiStatus(): State.Status = when (this) {
        is AdbStatus.Init -> State.Status.Init
        is AdbStatus.Loading -> State.Status.Loading(this.text)
        is AdbStatus.Success -> State.Status.Success
        is AdbStatus.Fail -> State.Status.Fail(this.text)
    }
}

/**
 * 为了保持 API 兼容性，保留原有的 State 类结构
 */
data class State(
    val status: Status = Status.Init,
    val adbToolPath: String = "",
    val adbToolAvailable: Boolean = false,
    val connectingDevices: List<String> = emptyList(),
    val selectedDevice: String = "",
    val adbParentPath: String? = null
) {
    sealed class Status(open val text: String = "") {
        object Init : Status("")
        data class Loading(override val text: String) : Status(text)
        data object Success : Status("")
        data class Fail(override val text: String) : Status(text)
    }
}