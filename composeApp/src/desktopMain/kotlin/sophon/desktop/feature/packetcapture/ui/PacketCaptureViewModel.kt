package sophon.desktop.feature.packetcapture.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import sophon.desktop.core.CACHE_HOME
import sophon.desktop.core.Context
import sophon.desktop.feature.packetcapture.data.repository.PacketCaptureRepository
import sophon.desktop.feature.packetcapture.data.repository.PacketCaptureRepositoryImpl
import sophon.desktop.feature.packetcapture.data.source.grpc.ProtobufSchemaRegistry
import sophon.desktop.feature.packetcapture.model.CaptureState
import sophon.desktop.feature.packetcapture.model.CaptureStatus
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import sophon.desktop.feature.packetcapture.model.ProtoPath
import sophon.desktop.feature.packetcapture.model.ThrottleConfig
import java.io.File

/**
 * 抓包功能的状态容器，持有 [CaptureState] 并将用户操作转发至 Repository 层。
 * 通过 [viewModelScope] 管理抓包协程生命周期，并监听设备切换事件刷新代理信息。
 * 生命周期结束时自动停止代理服务器。
 *
 * 同时管理 gRPC Schema 路径的持久化与加载。
 */
class PacketCaptureViewModel(
    private val repository: PacketCaptureRepository = PacketCaptureRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureState())
    val uiState = _uiState.asStateFlow()

    private var captureJob: Job? = null

    private val protoPathsFile = File("$CACHE_HOME/proto_paths.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        observeDeviceProxy()
        restoreProtoPaths()
    }

    // ─── 设备代理 ───────────────────────────────────────────────────────────

    private fun observeDeviceProxy() {
        viewModelScope.launch(Dispatchers.IO) {
            Context.stream.collect { refreshDeviceProxy() }
        }
    }

    private suspend fun refreshDeviceProxy() {
        val proxy = runCatching { repository.getDeviceProxy() }.getOrDefault("")
        _uiState.update { it.copy(deviceProxy = proxy) }
    }

    // ─── 抓包 ───────────────────────────────────────────────────────────────

    fun startCapture() {
        val state = _uiState.value
        if (state.isRunning) return

        _uiState.update { it.copy(status = CaptureStatus.RUNNING, errorMessage = null) }

        captureJob = viewModelScope.launch(Dispatchers.IO) {
            repository.startCapture(state.port).collect { packet ->
                _uiState.update { current ->
                    val newExpanded = if (packet.host !in current.expandedHosts)
                        current.expandedHosts + packet.host
                    else
                        current.expandedHosts
                    current.copy(
                        packets = current.packets + packet,
                        expandedHosts = newExpanded
                    )
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
        _uiState.update { it.copy(packets = emptyList(), selectedPacketId = null, expandedHosts = emptySet()) }
    }

    fun toggleHostExpanded(host: String) {
        _uiState.update { s ->
            val hosts = if (host in s.expandedHosts) s.expandedHosts - host else s.expandedHosts + host
            s.copy(expandedHosts = hosts)
        }
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

    // ─── 限速 ────────────────────────────────────────────────────────────────

    fun openThrottleDialog() {
        _uiState.update { it.copy(showThrottleDialog = true) }
    }

    fun closeThrottleDialog() {
        _uiState.update { it.copy(showThrottleDialog = false) }
    }

    fun updateThrottle(config: ThrottleConfig) {
        _uiState.update { it.copy(throttleConfig = config, showThrottleDialog = false) }
        repository.updateThrottle(config)
    }

    // ─── Proto Schema 管理 ──────────────────────────────────────────────────

    fun openProtoManager() {
        _uiState.update { it.copy(showProtoManager = true) }
    }

    fun closeProtoManager() {
        _uiState.update { it.copy(showProtoManager = false) }
    }

    /**
     * 添加 proto 路径（文件或目录），去重后持久化并触发 Schema 重载。
     */
    fun addProtoPath(path: String, isDirectory: Boolean) {
        val newPath = ProtoPath(path, isDirectory)
        _uiState.update { s ->
            if (s.protoPaths.any { it.path == path }) return@update s
            s.copy(protoPaths = s.protoPaths + newPath)
        }
        saveProtoPaths()
        reloadProtoSchema()
    }

    /**
     * 移除指定路径，持久化并触发 Schema 重载。
     */
    fun removeProtoPath(path: String) {
        _uiState.update { s -> s.copy(protoPaths = s.protoPaths.filter { it.path != path }) }
        saveProtoPaths()
        reloadProtoSchema()
    }

    /**
     * 强制重新加载所有已配置路径的 Schema。
     */
    fun reloadProtoSchema() {
        val paths = _uiState.value.protoPaths
        viewModelScope.launch(Dispatchers.IO) {
            val result = ProtobufSchemaRegistry.load(paths)
            _uiState.update {
                it.copy(
                    schemaLoadedCount = result.loadedCount,
                    schemaLoadError = result.error
                )
            }
        }
    }

    // ─── 持久化 ─────────────────────────────────────────────────────────────

    private fun restoreProtoPaths() {
        viewModelScope.launch(Dispatchers.IO) {
            val paths = runCatching {
                if (protoPathsFile.exists()) {
                    json.decodeFromString<List<ProtoPath>>(protoPathsFile.readText())
                } else emptyList()
            }.getOrDefault(emptyList())

            if (paths.isNotEmpty()) {
                _uiState.update { it.copy(protoPaths = paths) }
                val result = ProtobufSchemaRegistry.load(paths)
                _uiState.update {
                    it.copy(schemaLoadedCount = result.loadedCount, schemaLoadError = result.error)
                }
            }
        }
    }

    private fun saveProtoPaths() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                protoPathsFile.parentFile?.mkdirs()
                protoPathsFile.writeText(json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(ProtoPath.serializer()),
                    _uiState.value.protoPaths
                ))
            }
        }
    }

    override fun onCleared() {
        if (_uiState.value.isRunning) stopCapture()
        super.onCleared()
    }
}
