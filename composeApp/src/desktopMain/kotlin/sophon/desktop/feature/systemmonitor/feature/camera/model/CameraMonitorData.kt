package sophon.desktop.feature.systemmonitor.feature.camera.model

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

/**
 * DataSpace 值解释器
 *
 * 用于将 DataSpace 十六进制值转换为可读的名称
 */
object DataSpaceHelper {
    // 常见的 DataSpace 值映射
    private val dataSpaceMap = mapOf(
        "0x0" to "UNKNOWN",
        "0x8c20000" to "JFIF (sRGB)",
        "0x8c30000" to "BT601_625 (sRGB)",
        "0x8c40000" to "BT601_525 (sRGB)",
        "0x8c50000" to "BT709 (sRGB)",
        "0x90c0000" to "DCI_P3 (Display P3)",
        "0x8d00000" to "ADOBE_RGB",
        "0x8e00000" to "BT2020 (Rec.2020)",
        "0x8f00000" to "BT2020_PQ (HDR10)",
        "0x9000000" to "BT2020_HLG (HLG HDR)",
        "0x1000" to "DEPTH",
        "0x23" to "ARBITRARY"
    )

    /**
     * 根据 DataSpace 十六进制值获取可读名称
     *
     * @param dataSpace DataSpace 十六进制值 (如 0x8c20000)
     * @return 可读名称 (如 JFIF (sRGB))，如果未找到则返回原值
     */
    fun getDataSpaceName(dataSpace: String): String {
        val normalizedDataSpace = dataSpace.lowercase()
        return dataSpaceMap[normalizedDataSpace] ?: ""
    }

    /**
     * 获取带解释的 DataSpace 字符串
     *
     * @param dataSpace DataSpace 十六进制值
     * @return 格式化的字符串，如 "0x8c20000 (JFIF - sRGB)"
     */
    fun getFormattedDataSpace(dataSpace: String): String {
        val name = getDataSpaceName(dataSpace)
        return if (name.isNotEmpty()) {
            "$dataSpace ($name)"
        } else {
            dataSpace
        }
    }
}

/**
 * Usage 标志位解释器
 *
 * 用于将 GraphicBuffer Usage 十六进制值转换为可读的用途说明
 */
object UsageHelper {
    // Usage 标志位定义（可以组合）
    private val usageFlags = mapOf(
        0x00000001L to "SW_READ_NEVER",
        0x00000002L to "SW_READ_RARELY",
        0x00000003L to "SW_READ_OFTEN",
        0x00000010L to "SW_WRITE_NEVER",
        0x00000020L to "SW_WRITE_RARELY",
        0x00000030L to "SW_WRITE_OFTEN",
        0x00000100L to "HW_TEXTURE",
        0x00000200L to "HW_RENDER",
        0x00000400L to "HW_2D",
        0x00000800L to "HW_COMPOSER",
        0x00001000L to "HW_FB",
        0x00002000L to "EXTERNAL_DISP",
        0x00004000L to "PROTECTED",
        0x00008000L to "CURSOR",
        0x00010000L to "HW_VIDEO_ENCODER",
        0x00020000L to "HW_CAMERA_WRITE",
        0x00040000L to "HW_CAMERA_READ",
        0x00080000L to "HW_CAMERA_ZSL",
        0x00100000L to "HW_CAMERA_MASK",
        0x00200000L to "HW_MASK",
        0x20000000L to "RENDERSCRIPT",
        0x01000000L to "FOREIGN_BUFFERS",
        0x02000000L to "GPU_DATA_BUFFER"
    )

    /**
     * 解析 Usage 标志位
     *
     * @param usage Usage 十六进制字符串 (如 0x20000100)
     * @return 解析后的标志位列表
     */
    fun parseUsageFlags(usage: String): List<String> {
        val flags = mutableListOf<String>()
        
        // 移除 0x 前缀并转换为 Long
        val usageValue = try {
            usage.removePrefix("0x").toLong(16)
        } catch (e: Exception) {
            return emptyList()
        }

        // 检查每个标志位
        usageFlags.forEach { (flag, name) ->
            if ((usageValue and flag) == flag) {
                flags.add(name)
            }
        }

        return flags
    }

    /**
     * 获取带解释的 Usage 字符串
     *
     * @param usage Usage 十六进制值
     * @return 格式化的字符串，如 "0x20000100 (HW_TEXTURE | RENDERSCRIPT)"
     */
    fun getFormattedUsage(usage: String): String {
        val flags = parseUsageFlags(usage)
        return if (flags.isNotEmpty()) {
            "$usage (${flags.joinToString(" | ")})"
        } else {
            usage
        }
    }
}
