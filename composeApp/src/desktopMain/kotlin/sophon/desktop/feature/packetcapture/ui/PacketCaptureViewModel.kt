package sophon.desktop.feature.packetcapture.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import sophon.desktop.core.CACHE_HOME
import sophon.desktop.feature.packetcapture.data.repository.PacketCaptureRepository
import sophon.desktop.feature.packetcapture.data.repository.PacketCaptureRepositoryImpl
import sophon.desktop.feature.packetcapture.data.source.grpc.GrpcBodyDecoder
import sophon.desktop.feature.packetcapture.data.source.grpc.ProtobufSchemaRegistry
import sophon.desktop.feature.packetcapture.model.CaptureState
import sophon.desktop.feature.packetcapture.model.CaptureStatus
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import sophon.desktop.feature.packetcapture.model.DecodedBody
import sophon.desktop.feature.packetcapture.model.GrpcDecoded
import sophon.desktop.feature.packetcapture.model.ProtoPath
import sophon.desktop.feature.packetcapture.model.ThrottleConfig
import sophon.desktop.feature.packetcapture.model.fileDownloadInfo
import sophon.desktop.feature.packetcapture.model.isFileDownload
import sophon.desktop.feature.packetcapture.ui.PacketCaptureViewModel.Companion.JSON_SIZE_LIMIT
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
    /** 当前正在后台解码的 Job，新的选包请求到来时会先取消上一个。 */
    private var decodeJob: Job? = null

    private val protoPathsFile = File("$CACHE_HOME/proto_paths.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val prettyJson = Json { prettyPrint = true }

    init {
        restoreProtoPaths()
    }

    // ─── 抓包 ───────────────────────────────────────────────────────────────

    fun startCapture() {
        val state = _uiState.value
        if (state.isRunning) return

        _uiState.update { it.copy(status = CaptureStatus.RUNNING, errorMessage = null) }

        captureJob = viewModelScope.launch(Dispatchers.IO) {
            repository.startCapture(state.port).collect { packet ->
                _uiState.update { current ->
                    val isUpdate = current.packetIndex.containsKey(packet.id)
                    val newExpanded = if (!isUpdate && packet.host !in current.expandedHosts)
                        current.expandedHosts + packet.host
                    else
                        current.expandedHosts
                    // pending 包（statusCode=null）先 append；响应到达后以相同 id 的完整包替换
                    val newPackets = if (isUpdate)
                        current.packets.map { if (it.id == packet.id) packet else it }
                    else
                        current.packets + packet
                    current.copy(
                        packets = newPackets,
                        packetIndex = current.packetIndex + (packet.id to packet),
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
    }

    fun clearPackets() {
        decodeJob?.cancel()
        decodeJob = null
        // 删除文件下载响应的临时文件，避免磁盘空间泄漏
        _uiState.value.packets.forEach { it.responseBodyFile?.delete() }
        _uiState.update {
            it.copy(
                packets = emptyList(),
                selectedPacketId = null,
                expandedHosts = emptySet(),
                decodedBodies = emptyMap(),
                isDecodingBody = false,
                packetIndex = emptyMap(),
            )
        }
    }

    fun toggleHostExpanded(host: String) {
        _uiState.update { s ->
            val hosts = if (host in s.expandedHosts) s.expandedHosts - host else s.expandedHosts + host
            s.copy(expandedHosts = hosts)
        }
    }

    /**
     * 选中一条包记录。
     * - 若已存在解码缓存，直接使用，无需重新解码。
     * - 否则取消上一次解码任务，在后台线程完成解码后更新状态。
     */
    fun selectPacket(packet: CapturedPacket?) {
        decodeJob?.cancel()
        decodeJob = null

        val alreadyDecoded = packet != null && _uiState.value.decodedBodies.containsKey(packet.id)
        _uiState.update {
            it.copy(
                selectedPacketId = packet?.id,
                isDecodingBody = packet != null && !alreadyDecoded,
            )
        }
        if (packet == null || alreadyDecoded) return

        decodeJob = viewModelScope.launch(Dispatchers.Default) {
            val decoded = decodePacketBody(packet)
            if (!isActive) return@launch
            _uiState.update { s ->
                s.copy(
                    decodedBodies = s.decodedBodies + (packet.id to decoded),
                    isDecodingBody = if (s.selectedPacketId == packet.id) false else s.isDecodingBody,
                )
            }
        }
    }

    fun updateFilter(text: String) {
        _uiState.update { it.copy(filterText = text) }
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

    // ─── 文件保存 ────────────────────────────────────────────────────────────

    /**
     * 将选中包的响应体保存到用户指定路径。
     * 优先使用临时文件（文件下载场景，完整响应体），其次使用内存中的截断 body。
     * 对话框在主线程（Swing EDT）弹出，写文件在 IO 线程执行。
     */
    fun saveResponseBodyToFile(packet: CapturedPacket) {
        val sourceFile = packet.responseBodyFile
        val body = packet.responseBody
        if (sourceFile == null && body == null) return
        viewModelScope.launch(Dispatchers.Main) {
            val suggestedName = _uiState.value.decodedBodies[packet.id]?.fileInfo?.fileName
                ?: packet.path.substringAfterLast('/').substringBefore('?').ifBlank { "response" }
            val chooser = javax.swing.JFileChooser().apply {
                selectedFile = java.io.File(suggestedName)
                dialogTitle = "保存响应体"
            }
            if (chooser.showSaveDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        sourceFile?.copyTo(chooser.selectedFile, overwrite = true)
                            ?: chooser.selectedFile.writeBytes(body!!)
                    }.onFailure { e ->
                        _uiState.update { it.copy(errorMessage = "保存失败：${e.message}") }
                    }
                }
            }
        }
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
        _uiState.value.packets.forEach { it.responseBodyFile?.delete() }
        super.onCleared()
    }

    // ─── 私有：后台 body 解码 ────────────────────────────────────────────────

    /**
     * 在 [Dispatchers.Default] 上执行所有耗时的 body 解码操作：
     * - 文件下载：仅提取头部元信息，不解析 body。
     * - gRPC：调用 [GrpcBodyDecoder]。
     * - 普通 HTTP：gzip/deflate 解压 + JSON 解析 + pretty-print。
     *
     * JSON body 超过 [JSON_SIZE_LIMIT] 字符时跳过 [JsonElement] 解析和 pretty-print，
     * 避免超大 JSON 消耗过多内存。
     */
    private fun decodePacketBody(packet: CapturedPacket): DecodedBody {
        if (packet.isFileDownload()) {
            return DecodedBody(
                isFileDownload = true,
                fileInfo = packet.fileDownloadInfo(),
                bodyAvailable = packet.responseBodyFile != null || packet.responseBody != null,
            )
        }

        if (packet is CapturedPacket.Grpc) {
            val grpcReq = packet.requestBody?.let { body ->
                runCatching {
                    val result = GrpcBodyDecoder.decode(body, packet.path, isRequest = true)
                    val formatted = if (result.isSchemaApplied) {
                        runCatching {
                            val element = Json.parseToJsonElement(result.body)
                            prettyJson.encodeToString(JsonElement.serializer(), element)
                        }.getOrNull()
                    } else null
                    GrpcDecoded(result.body, result.isSchemaApplied, formatted)
                }.getOrNull()
            }
            val grpcResp = packet.responseBody?.let { body ->
                runCatching {
                    val result = GrpcBodyDecoder.decode(body, packet.path, isRequest = false)
                    val formatted = if (result.isSchemaApplied) {
                        runCatching {
                            val element = Json.parseToJsonElement(result.body)
                            prettyJson.encodeToString(JsonElement.serializer(), element)
                        }.getOrNull()
                    } else null
                    GrpcDecoded(result.body, result.isSchemaApplied, formatted)
                }.getOrNull()
            }
            return DecodedBody(
                requestText = packet.requestBodyAsText(),
                responseText = packet.responseBodyAsText(),
                grpcRequest = grpcReq,
                grpcResponse = grpcResp,
            )
        }

        // 普通 HTTP
        val reqText = packet.requestBodyAsText()
        val respText = packet.responseBodyAsText()

        val reqJson: JsonElement?
        val reqPretty: String?
        if (!reqText.isNullOrEmpty() && reqText.length <= JSON_SIZE_LIMIT) {
            reqJson = runCatching { Json.parseToJsonElement(reqText) }.getOrNull()
            reqPretty = reqJson?.let {
                runCatching { prettyJson.encodeToString(JsonElement.serializer(), it) }.getOrNull()
            }
        } else {
            reqJson = null
            reqPretty = null
        }

        val respJson: JsonElement?
        val respPretty: String?
        if (!respText.isNullOrEmpty() && respText.length <= JSON_SIZE_LIMIT) {
            respJson = runCatching { Json.parseToJsonElement(respText) }.getOrNull()
            respPretty = respJson?.let {
                runCatching { prettyJson.encodeToString(JsonElement.serializer(), it) }.getOrNull()
            }
        } else {
            respJson = null
            respPretty = null
        }

        return DecodedBody(
            requestText = reqText,
            responseText = respText,
            requestJson = reqJson,
            responseJson = respJson,
            requestPrettyJson = reqPretty,
            responsePrettyJson = respPretty,
        )
    }

    companion object {
        /** JSON body 超过此字符数时跳过 JsonElement 解析，降级为纯文本展示。 */
        const val JSON_SIZE_LIMIT = 200_000
    }
}
