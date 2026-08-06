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
import sophon.desktop.feature.packetcapture.model.GrpcMockRule
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
    private val mockRulesFile = File("$CACHE_HOME/grpc_mock_rules.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val prettyJson = Json { prettyPrint = true }

    init {
        // 启动时按顺序：恢复 proto 路径→加载 Schema→恢复规则→编码，保证规则立即生效
        viewModelScope.launch(Dispatchers.IO) {
            restoreProtoPathsInternal()
            restoreMockRulesInternal()
            // Schema 已加载：重编码使规则立即生效；Schema 缺失时编码失败的规则静默跳过（不报错）
            reEncodeMockRulesQuiet()
        }
        _uiState.update { it.copy(caCertPath = repository.getCaCertPath()) }
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
            // 尝试 ADB 推送（仅对 Android 有效），不论是否成功都弹出引导对话框，
            // iOS 用户同样可以通过对话框中的步骤完成手动安装。
            val pushed = runCatching { repository.installCaToDevice(); true }.getOrDefault(false)
            _uiState.update { it.copy(showCaInstallGuide = true, caAndroidPushed = pushed) }
        }
    }

    /**
     * 在 Finder（macOS）/ 文件管理器（Windows）中定位 CA 证书文件，
     * 方便用户通过 AirDrop 等方式将其传输至 iOS 设备。
     */
    fun revealCaCertInFinder() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val certFile = File(_uiState.value.caCertPath)
                java.awt.Desktop.getDesktop().open(certFile.parentFile)
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
                selectedFile = File(suggestedName)
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
     * 强制重新加载所有已配置路径的 Schema。Schema 加载完成后自动重编 Mock 规则。
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
            // Schema 更新后重新编码所有 mock 规则
            reEncodeMockRules()
        }
    }

    // ─── 持久化 ─────────────────────────────────────────────────────────────

    private fun restoreProtoPaths() {
        viewModelScope.launch(Dispatchers.IO) { restoreProtoPathsInternal() }
    }

    private suspend fun restoreProtoPathsInternal() {
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

    // ─── gRPC Mock 管理 ──────────────────────────────────────────────────────

    fun openMockDialog() {
        _uiState.update { it.copy(showMockDialog = true, editingMockRule = null) }
    }

    fun closeMockDialog() {
        _uiState.update { it.copy(showMockDialog = false, editingMockRule = null) }
    }

    fun startEditMockRule(rule: GrpcMockRule?) {
        _uiState.update { it.copy(editingMockRule = rule) }
    }

    /**
     * 保存（新增或更新）一条 Mock 规则。立即尝试编码并推送到 [GrpcMockRegistry]。
     * [fromPacket] 为 true 时表示从抓包列表快捷添加，自动打开对话框并选中新建规则。
     */
    fun saveMockRule(rule: GrpcMockRule, fromPacket: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val encodeResult = ProtobufSchemaRegistry.encodeFromJson(rule.responseJson, rule.path)
            val encodeError = encodeResult.exceptionOrNull()?.message
            val encodedBody = encodeResult.getOrNull()

            _uiState.update { s ->
                val existingById = s.mockRules.any { it.id == rule.id }
                val newRules = when {
                    existingById ->
                        // 按 ID 更新（普通编辑保存）
                        s.mockRules.map { if (it.id == rule.id) rule else it }
                    s.mockRules.any { it.host == rule.host && it.path == rule.path } ->
                        // 相同 host+path 已存在（重复添加），原地替换以去重
                        s.mockRules.map { if (it.host == rule.host && it.path == rule.path) rule.copy(id = it.id) else it }
                    else ->
                        s.mockRules + rule
                }
                val newErrors = if (encodeError != null) s.mockEncodeErrors + (rule.id to encodeError)
                                else s.mockEncodeErrors - rule.id
                s.copy(
                    mockRules = newRules,
                    mockEncodeErrors = newErrors,
                    editingMockRule = null,
                    showMockDialog = if (fromPacket) true else s.showMockDialog,
                )
            }
            saveMockRulesToDisk()
            pushMockRulesToRegistry()
        }
    }

    fun deleteMockRule(ruleId: String) {
        _uiState.update { s ->
            s.copy(
                mockRules = s.mockRules.filter { it.id != ruleId },
                mockEncodeErrors = s.mockEncodeErrors - ruleId,
                editingMockRule = if (s.editingMockRule?.id == ruleId) null else s.editingMockRule,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            saveMockRulesToDisk()
            pushMockRulesToRegistry()
        }
    }

    fun toggleMockRule(ruleId: String) {
        _uiState.update { s ->
            s.copy(mockRules = s.mockRules.map {
                if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
            })
        }
        viewModelScope.launch(Dispatchers.IO) {
            saveMockRulesToDisk()
            pushMockRulesToRegistry()
        }
    }

    /**
     * 从已选中的 gRPC 抓包记录快捷创建 Mock 规则，预填 host/path 和响应 JSON。
     */
    fun addMockFromPacket(packet: CapturedPacket.Grpc) {
        val decodedBody = _uiState.value.decodedBodies[packet.id]
        val prefilledJson = decodedBody?.grpcResponse
            ?.let { decoded ->
                val raw = decoded.formattedBody ?: decoded.body ?: return@let null
                // formattedBody 以 "[压缩: ... | 长度: ... bytes]\n" 开头，剥掉第一行只保留 JSON
                if (raw.startsWith("[")) raw.substringAfter('\n') else raw
            }
            ?: "{}"
        // 若已存在相同 host+path 的规则，直接编辑旧条目（更新 JSON），避免重复
        val existing = _uiState.value.mockRules.find { it.host == packet.host && it.path == packet.path }
        val rule = existing?.copy(responseJson = prefilledJson)
            ?: GrpcMockRule(host = packet.host, path = packet.path, responseJson = prefilledJson)
        _uiState.update { it.copy(editingMockRule = rule, showMockDialog = true) }
    }

    /** 从磁盘恢复 Mock 规则（仅加载到 state，不做编码）。 */
    private fun restoreMockRules() {
        viewModelScope.launch(Dispatchers.IO) { restoreMockRulesInternal() }
    }

    private suspend fun restoreMockRulesInternal() {
        val rules = runCatching {
            if (mockRulesFile.exists())
                json.decodeFromString<List<GrpcMockRule>>(mockRulesFile.readText())
            else emptyList()
        }.getOrDefault(emptyList())
        if (rules.isEmpty()) return
        _uiState.update { it.copy(mockRules = rules) }
    }

    /** 对当前所有规则重新编码（Schema 刷新后调用）。 */
    private fun reEncodeMockRules() {
        val rules = _uiState.value.mockRules
        val errors = mutableMapOf<String, String>()
        val encoded = mutableMapOf<String, ByteArray>()
        for (rule in rules) {
            val result = ProtobufSchemaRegistry.encodeFromJson(rule.responseJson, rule.path)
            result.onSuccess { encoded[rule.id] = it }
            result.onFailure { errors[rule.id] = it.message ?: "编码失败" }
        }
        _uiState.update { it.copy(mockEncodeErrors = errors) }
        repository.updateMockRules(rules, encoded)
    }

    /**
     * 启动时静默编码：规则推入 Registry 使其立即生效；
     * 编码失败（Schema 缺失/JSON 非法）的规则静默跳过，不写入 UI 错误。
     * 错误提示留给用户主动打开 Mock 对话框或重载 Schema 时再触发。
     */
    private fun reEncodeMockRulesQuiet() {
        val rules = _uiState.value.mockRules
        if (rules.isEmpty()) return
        val encoded = mutableMapOf<String, ByteArray>()
        for (rule in rules) {
            ProtobufSchemaRegistry.encodeFromJson(rule.responseJson, rule.path)
                .onSuccess { encoded[rule.id] = it }
        }
        repository.updateMockRules(rules, encoded)
    }

    /** 将 [_uiState] 中当前规则推送到 Registry（不重编码，仅用于 toggle/delete 后的快速同步）。 */
    private fun pushMockRulesToRegistry() {
        val rules = _uiState.value.mockRules
        val errors = mutableMapOf<String, String>()
        val encoded = mutableMapOf<String, ByteArray>()
        for (rule in rules) {
            val result = ProtobufSchemaRegistry.encodeFromJson(rule.responseJson, rule.path)
            result.onSuccess { encoded[rule.id] = it }
            result.onFailure { errors[rule.id] = it.message ?: "编码失败" }
        }
        _uiState.update { s -> s.copy(mockEncodeErrors = errors) }
        repository.updateMockRules(rules, encoded)
    }

    private fun saveMockRulesToDisk() {
        runCatching {
            mockRulesFile.parentFile?.mkdirs()
            mockRulesFile.writeText(json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(GrpcMockRule.serializer()),
                _uiState.value.mockRules
            ))
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
                    val (element, formatted) = if (result.isSchemaApplied) {
                        // result.body 带帧头前缀，剥掉第一行再解析
                        val jsonText = if (result.body.startsWith("[")) result.body.substringAfter('\n') else result.body
                        val el = runCatching { Json.parseToJsonElement(jsonText) }.getOrNull()
                        val fmt = el?.let { runCatching { prettyJson.encodeToString(JsonElement.serializer(), it) }.getOrNull() }
                        el to fmt
                    } else null to null
                    GrpcDecoded(result.body, result.isSchemaApplied, formatted, element)
                }.getOrNull()
            }
            val grpcResp = packet.responseBody?.let { body ->
                runCatching {
                    val result = GrpcBodyDecoder.decode(body, packet.path, isRequest = false)
                    val (element, formatted) = if (result.isSchemaApplied) {
                        val jsonText = if (result.body.startsWith("[")) result.body.substringAfter('\n') else result.body
                        val el = runCatching { Json.parseToJsonElement(jsonText) }.getOrNull()
                        val fmt = el?.let { runCatching { prettyJson.encodeToString(JsonElement.serializer(), it) }.getOrNull() }
                        el to fmt
                    } else null to null
                    GrpcDecoded(result.body, result.isSchemaApplied, formatted, element)
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
