package sophon.desktop.feature.packetcapture.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Context
import sophon.desktop.feature.packetcapture.data.repository.PacketCaptureRepository
import sophon.desktop.feature.packetcapture.data.repository.PacketCaptureRepositoryImpl
import sophon.desktop.feature.packetcapture.model.CaptureState
import sophon.desktop.feature.packetcapture.model.CaptureStatus
import sophon.desktop.feature.packetcapture.model.CapturedPacket

/**
 * 抓包功能的状态容器，持有 [CaptureState] 并将用户操作转发至 Repository 层。
 * 通过 [viewModelScope] 管理抓包协程生命周期，并监听设备切换事件刷新代理信息。
 * 生命周期结束时自动停止代理服务器。
 */
class PacketCaptureViewModel(
    private val repository: PacketCaptureRepository = PacketCaptureRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureState())
    val uiState = _uiState.asStateFlow()

    private var captureJob: Job? = null

    init {
        observeDeviceProxy()
    }

    private fun observeDeviceProxy() {
        viewModelScope.launch(Dispatchers.IO) {
            Context.stream.collect {
                refreshDeviceProxy()
            }
        }
    }

    private suspend fun refreshDeviceProxy() {
        val proxy = runCatching { repository.getDeviceProxy() }.getOrDefault("")
        _uiState.update { it.copy(deviceProxy = proxy) }
    }

    fun startCapture() {
        val state = _uiState.value
        if (state.isRunning) return

        _uiState.update { it.copy(status = CaptureStatus.RUNNING, errorMessage = null) }

        captureJob = viewModelScope.launch(Dispatchers.IO) {
            repository.startCapture(state.port).collect { packet ->
                _uiState.update { current ->
                    current.copy(packets = current.packets + packet)
                }
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                if (cause != null && cause !is kotlinx.coroutines.CancellationException) {
                    _uiState.update {
                        it.copy(
                            status = CaptureStatus.ERROR,
                            errorMessage = cause.message ?: "抓包出现错误"
                        )
                    }
                }
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        repository.stopCapture()
        _uiState.update { it.copy(status = CaptureStatus.STOPPED) }
        viewModelScope.launch(Dispatchers.IO) { refreshDeviceProxy() }
    }

    fun clearPackets() {
        _uiState.update { it.copy(packets = emptyList(), selectedPacketId = null) }
    }

    fun selectPacket(packet: CapturedPacket?) {
        _uiState.update { it.copy(selectedPacketId = packet?.id) }
    }

    fun updateFilter(text: String) {
        _uiState.update { it.copy(filterText = text) }
    }

    fun updatePort(port: Int) {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(port = port) }
    }

    fun installCaToDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.installCaToDevice()
                _uiState.update { it.copy(showCaInstallGuide = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = "CA 安装失败: ${e.message}") }
            }
        }
    }

    fun dismissCaInstallGuide() {
        _uiState.update { it.copy(showCaInstallGuide = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, status = CaptureStatus.STOPPED) }
    }

    override fun onCleared() {
        if (_uiState.value.isRunning) stopCapture()
        super.onCleared()
    }
}
