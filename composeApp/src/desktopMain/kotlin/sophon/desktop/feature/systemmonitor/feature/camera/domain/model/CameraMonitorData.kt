package sophon.desktop.feature.systemmonitor.feature.camera.domain.model

/**
 * 相机监控数据汇总
 *
 * @param eventLogs 相机服务事件日志
 * @param deviceInfoList 相机设备动态信息列表
 */
data class CameraData(
    val eventLogs: List<CameraEventLog> = emptyList(),
    val deviceInfoList: List<CameraDeviceInfo> = emptyList()
)

/**
 * 相机事件日志
 *
 * @param timestamp 时间戳 (MM-dd HH:mm:ss 格式)
 * @param eventType 事件类型 (CONNECT, DISCONNECT, DIED)
 * @param deviceId 设备ID
 * @param packageName 应用包名
 * @param pid 进程ID
 * @param reason 原因（仅 DIED 事件有）
 */
data class CameraEventLog(
    val timestamp: String = "",
    val eventType: CameraEventType = CameraEventType.UNKNOWN,
    val deviceId: String = "",
    val packageName: String = "",
    val pid: Int = 0,
    val reason: String = ""
)

/**
 * 相机事件类型枚举
 */
enum class CameraEventType {
    CONNECT,
    DISCONNECT,
    DIED,
    UNKNOWN
}

/**
 * 相机设备动态信息
 *
 * @param deviceId 设备ID
 * @param isOpen 是否打开
 * @param clientInfo 客户端信息
 * @param cameraState 相机状态 (PREVIEW, RECORDING, IDLE 等)
 * @param previewConfig 预览配置
 * @param captureConfig 拍照配置
 * @param videoConfig 视频配置
 * @param streamList 活跃的流列表
 * @param frameStats 帧统计信息
 */
data class CameraDeviceInfo(
    val deviceId: String = "",
    val isOpen: Boolean = false,
    val clientInfo: CameraClientDetail? = null,
    val cameraState: String = "",
    val previewConfig: CameraStreamConfig = CameraStreamConfig(),
    val captureConfig: CameraStreamConfig = CameraStreamConfig(),
    val videoConfig: CameraStreamConfig = CameraStreamConfig(),
    val streamList: List<CameraStream> = emptyList(),
    val frameStats: CameraFrameStats = CameraFrameStats()
)

/**
 * 客户端详细信息
 *
 * @param pid 进程ID
 * @param packageName 应用包名
 * @param priorityScore 优先级分数
 * @param state 状态码
 */
data class CameraClientDetail(
    val pid: Int = 0,
    val packageName: String = "",
    val priorityScore: Int = 0,
    val state: Int = 0
)

/**
 * 相机流配置
 *
 * @param width 宽度
 * @param height 高度
 * @param fpsMin 最小帧率
 * @param fpsMax 最大帧率
 */
data class CameraStreamConfig(
    val width: Int = 0,
    val height: Int = 0,
    val fpsMin: Int = 0,
    val fpsMax: Int = 0
) {
    /**
     * 获取分辨率字符串
     */
    val resolution: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else "N/A"

    /**
     * 获取帧率字符串
     */
    val fpsRange: String
        get() = if (fpsMin > 0 || fpsMax > 0) "$fpsMin - $fpsMax fps" else "N/A"
}

/**
 * 相机流信息
 *
 * (来自 Stream configuration 部分)
 *
 * @param streamId 流ID
 * @param type 类型 (Output/Input)
 * @param consumerName 消费者名称
 * @param width 宽度
 * @param height 高度
 * @param format 格式 (如 0x22 = YUV_420_888)
 * @param formatName 格式名称 (人类可读)
 * @param dataSpace 数据空间
 * @param rotation 旋转角度
 * @param usage 用途标志
 * @param framesProduced 已产生的帧数
 * @param lastTimestamp 最后一帧的时间戳 (纳秒)
 * @param calculatedFps 计算得出的实时帧率
 */
data class CameraStream(
    val streamId: Int = 0,
    val type: String = "",
    val consumerName: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val format: String = "",
    val formatName: String = "",
    val dataSpace: String = "",
    val rotation: Int = 0,
    val usage: String = "",
    val framesProduced: Long = 0,
    val lastTimestamp: Long = 0,
    val calculatedFps: Float = 0f
) {
    /**
     * 获取分辨率字符串
     */
    val resolution: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else "N/A"
}

/**
 * 相机帧统计信息
 *
 * @param totalFrames 总帧数
 * @param currentFps 当前帧率（计算得出）
 * @param deviceStatus 设备状态 (ACTIVE, IDLE 等)
 */
data class CameraFrameStats(
    val totalFrames: Long = 0,
    val currentFps: Float = 0f,
    val deviceStatus: String = ""
)

/**
 * 常见的像素格式映射
 */
object PixelFormatHelper {
    private val formatMap = mapOf(
        "0x1" to "RGBA_8888",
        "0x2" to "RGBX_8888",
        "0x3" to "RGB_888",
        "0x4" to "RGB_565",
        "0x5" to "BGRA_8888",
        "0x11" to "NV16",
        "0x21" to "NV21",
        "0x22" to "YUV_420_888",
        "0x23" to "YUV_422_888",
        "0x24" to "YUV_444_888",
        "0x25" to "IMPLEMENTATION_DEFINED",
        "0x32315659" to "YV12",
        "0x100" to "RAW_SENSOR",
        "0x20" to "RAW10",
        "0x25" to "RAW12",
        "0x26" to "RAW_OPAQUE",
        "0x21" to "BLOB",
        "0x41" to "JPEG",
        "0x44363050" to "RAW_DEPTH"
    )

    /**
     * 根据格式代码获取格式名称
     *
     * @param format 格式代码 (如 0x22)
     * @return 格式名称 (如 YUV_420_888)
     */
    fun getFormatName(format: String): String {
        val normalizedFormat = format.lowercase()
        return formatMap[normalizedFormat] ?: format
    }
}
