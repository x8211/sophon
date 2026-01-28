package sophon.desktop.feature.systemmonitor.feature.camera.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraClientDetail
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraData
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraDeviceInfo
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraEventLog
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraEventType
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraFrameStats
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraStream
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraStreamConfig
import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.PixelFormatHelper
import sophon.desktop.feature.systemmonitor.feature.camera.domain.repository.CameraRepository

/**
 * 相机监控数据仓库实现
 *
 * 通过 ADB Shell 命令 `dumpsys media.camera` 获取设备的相机服务状态信息
 */
class CameraRepositoryImpl : CameraRepository {

    /**
     * ADB 命令：获取相机服务信息
     */
    private val cameraCommand = "adb shell dumpsys media.camera"

    /**
     * 上一次的流帧数记录，用于计算帧率
     * Key: "deviceId_streamId", Value: Pair<帧数, 时间戳毫秒>
     */
    private val lastFrameCountMap = mutableMapOf<String, Pair<Long, Long>>()

    override suspend fun getCameraData(): CameraData {
        return try {
            cameraCommand.oneshotShell { output -> parseCameraInfo(output) }
        } catch (e: Exception) {
            e.printStackTrace()
            CameraData()
        }
    }

    /**
     * 解析相机服务信息输出
     *
     * @param output dumpsys media.camera 的完整输出
     * @return 解析后的相机数据
     */
    private fun parseCameraInfo(output: String): CameraData {
        val lines = output.lines()
        val currentTimeMs = System.currentTimeMillis()

        // 1. 解析事件日志
        val eventLogs = parseEventLogs(lines)

        // 2. 解析设备动态信息（过滤掉之前会话的数据）
        val deviceInfoList = parseDeviceInfoList(output, currentTimeMs)

        return CameraData(
            eventLogs = eventLogs,
            deviceInfoList = deviceInfoList
        )
    }

    /**
     * 解析事件日志
     *
     * 示例:
     *   01-28 16:36:56 : CONNECT device 0 client for package com.mico (PID 10719)
     *   01-28 16:36:56 : DISCONNECT device 0 client for package com.mico (PID 10719)
     *   01-28 16:28:38 : DIED client(s) with PID 1629, reason: (Binder died unexpectedly)
     */
    private fun parseEventLogs(lines: List<String>): List<CameraEventLog> {
        val logs = mutableListOf<CameraEventLog>()

        // CONNECT/DISCONNECT 事件模式
        val connectPattern = Regex(
            """(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})\s*:\s*(CONNECT|DISCONNECT)\s+device\s+(\d+)\s+client\s+for\s+package\s+(\S+)\s+\(PID\s+(\d+)\)"""
        )

        // DIED 事件模式
        val diedPattern = Regex(
            """(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})\s*:\s*DIED\s+client\(s\)\s+with\s+PID\s+(\d+),\s+reason:\s*\((.+?)\)"""
        )

        for (line in lines) {
            val trimmedLine = line.trim()

            // 尝试匹配 CONNECT/DISCONNECT
            val connectMatch = connectPattern.find(trimmedLine)
            if (connectMatch != null) {
                val eventType = when (connectMatch.groupValues[2]) {
                    "CONNECT" -> CameraEventType.CONNECT
                    "DISCONNECT" -> CameraEventType.DISCONNECT
                    else -> CameraEventType.UNKNOWN
                }
                logs.add(
                    CameraEventLog(
                        timestamp = connectMatch.groupValues[1],
                        eventType = eventType,
                        deviceId = connectMatch.groupValues[3],
                        packageName = connectMatch.groupValues[4],
                        pid = connectMatch.groupValues[5].toIntOrNull() ?: 0
                    )
                )
                continue
            }

            // 尝试匹配 DIED
            val diedMatch = diedPattern.find(trimmedLine)
            if (diedMatch != null) {
                logs.add(
                    CameraEventLog(
                        timestamp = diedMatch.groupValues[1],
                        eventType = CameraEventType.DIED,
                        deviceId = "",
                        packageName = "",
                        pid = diedMatch.groupValues[2].toIntOrNull() ?: 0,
                        reason = diedMatch.groupValues[3]
                    )
                )
            }
        }

        return logs
    }

    /**
     * 解析设备动态信息列表
     *
     * 解析 "== Camera device X dynamic info: ==" 部分
     * 会过滤掉 "previous open session" 的数据
     *
     * @param output 原始输出
     * @param currentTimeMs 当前时间戳（毫秒）
     */
    private fun parseDeviceInfoList(output: String, currentTimeMs: Long): List<CameraDeviceInfo> {
        val deviceInfoList = mutableListOf<CameraDeviceInfo>()

        // 过滤掉之前会话的数据
        val input = filterPreviousSession(output)

        // 按设备分割
        val devicePattern = Regex("""==\s*Camera device\s+(\d+)\s+dynamic info:\s*==""")
        val matches = devicePattern.findAll(input).toList()

        for (i in matches.indices) {
            val match = matches[i]
            val deviceId = match.groupValues[1]
            val startIndex = match.range.last + 1

            // 找到当前设备块的结束位置
            val endIndex = if (i + 1 < matches.size) {
                matches[i + 1].range.first
            } else {
                // 查找下一个 "==" 开头的段落或文件末尾
                val nextSection = input.indexOf("== Camera", startIndex)
                if (nextSection > 0) nextSection else input.length
            }

            val deviceBlock = input.substring(startIndex, endIndex)

            val deviceInfo = parseDeviceInfo(deviceId, deviceBlock, currentTimeMs)
            deviceInfoList.add(deviceInfo)
        }

        return deviceInfoList
    }

    /**
     * 过滤掉之前会话的数据
     *
     * 之前会话的格式：
     * 开头：**********Dumpsys from previous open session**********
     * 结尾：**********End of Dumpsys from previous open session**********
     *
     * 删除开头和结尾标记之间的所有内容
     *
     * @param block 设备信息块
     * @return 过滤后的内容
     */
    private fun filterPreviousSession(block: String): String {
        // 使用正则匹配开头和结尾标记之间的内容并删除
        val previousSessionBlockPattern = Regex(
            """\*+[^\n]*[Pp]revious[^\n]*[Ss]ession[^\n]*\*+[\s\S]*?\*+[^\n]*[Ee]nd[^\n]*[Pp]revious[^\n]*[Ss]ession[^\n]*\*+""",
            RegexOption.MULTILINE
        )

        return previousSessionBlockPattern.replace(block, "")
    }

    /**
     * 解析单个设备的动态信息
     *
     * @param deviceId 设备ID
     * @param block 设备信息块（已过滤之前会话）
     * @param currentTimeMs 当前时间戳（毫秒）
     * @return 设备动态信息
     */
    private fun parseDeviceInfo(
        deviceId: String,
        block: String,
        currentTimeMs: Long
    ): CameraDeviceInfo {
        // 检查设备是否关闭
        val isClosedPattern = Regex("""Device\s+\d+\s+is\s+closed""")
        if (isClosedPattern.containsMatchIn(block)) {
            return CameraDeviceInfo(
                deviceId = deviceId,
                isOpen = false
            )
        }

        // 设备是打开的，解析详细信息
        val clientInfo = parseClientDetail(block)
        val cameraState = parseCameraState(block)
        val previewConfig = parsePreviewConfig(block)
        val captureConfig = parseCaptureConfig(block)
        val videoConfig = parseVideoConfig(block)
        val streamList = parseStreamList(block, deviceId, currentTimeMs)
        val frameStats = parseFrameStats(block, streamList)

        return CameraDeviceInfo(
            deviceId = deviceId,
            isOpen = true,
            clientInfo = clientInfo,
            cameraState = cameraState,
            previewConfig = previewConfig,
            captureConfig = captureConfig,
            videoConfig = videoConfig,
            streamList = streamList,
            frameStats = frameStats
        )
    }

    /**
     * 解析客户端详细信息
     *
     * 示例:
     * Client priority score: 0 state: 2
     * Client PID: 10719
     * Client package: com.mico
     */
    private fun parseClientDetail(block: String): CameraClientDetail? {
        val scorePattern = Regex("""Client priority score:\s*(\d+)\s+state:\s*(\d+)""")
        val pidPattern = Regex("""Client PID:\s*(\d+)""")
        val packagePattern = Regex("""Client package:\s*(\S+)""")

        val scoreMatch = scorePattern.find(block)
        val pidMatch = pidPattern.find(block)
        val packageMatch = packagePattern.find(block)

        if (pidMatch == null && packageMatch == null) return null

        return CameraClientDetail(
            pid = pidMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            packageName = packageMatch?.groupValues?.get(1) ?: "",
            priorityScore = scoreMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            state = scoreMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
        )
    }

    /**
     * 解析相机状态
     *
     * 示例: State: PREVIEW
     */
    private fun parseCameraState(block: String): String {
        val statePattern = Regex("""\s+State:\s+(\w+)""")
        return statePattern.find(block)?.groupValues?.get(1) ?: ""
    }

    /**
     * 解析预览配置
     *
     * 示例:
     * Preview size: 1280 x 720
     * Preview FPS range: 15 - 30
     */
    private fun parsePreviewConfig(block: String): CameraStreamConfig {
        val sizePattern = Regex("""Preview size:\s*(\d+)\s*x\s*(\d+)""")
        val fpsPattern = Regex("""Preview FPS range:\s*(\d+)\s*-\s*(\d+)""")

        val sizeMatch = sizePattern.find(block)
        val fpsMatch = fpsPattern.find(block)

        return CameraStreamConfig(
            width = sizeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            height = sizeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0,
            fpsMin = fpsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            fpsMax = fpsMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
        )
    }

    /**
     * 解析拍照配置
     *
     * 示例: Picture size: 4032 x 3024
     */
    private fun parseCaptureConfig(block: String): CameraStreamConfig {
        val sizePattern = Regex("""Picture size:\s*(\d+)\s*x\s*(\d+)""")
        val sizeMatch = sizePattern.find(block)

        return CameraStreamConfig(
            width = sizeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            height = sizeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0,
            fpsMin = 0,
            fpsMax = 0
        )
    }

    /**
     * 解析视频配置
     *
     * 示例: Video size: 1920 x 1080
     */
    private fun parseVideoConfig(block: String): CameraStreamConfig {
        val sizePattern = Regex("""Video size:\s*(\d+)\s*x\s*(\d+)""")
        val sizeMatch = sizePattern.find(block)

        return CameraStreamConfig(
            width = sizeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            height = sizeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0,
            fpsMin = 0,
            fpsMax = 0
        )
    }

    /**
     * 解析流列表（丰富版本）
     *
     * 示例:
     * Stream[0]: Output
     *   Consumer name: SurfaceTexture-1-10719-12
     *   State: 1
     *   Dims: 1280 x 720, format 0x22, dataspace 0x8c20000
     *   Max size: 1280 x 720
     *   Combined usage: 0x20000100, max HAL buffers: 8
     *   Frames produced: 1459, last timestamp: 454546233237780 ns
     *   Total buffers: 9, currently dequeued: 2
     *   DequeueLatency: min/max/avg = 0/0/0 us
     *   ...
     *
     * @param block 设备信息块
     * @param deviceId 设备ID
     * @param currentTimeMs 当前时间戳
     */
    private fun parseStreamList(
        block: String,
        deviceId: String,
        currentTimeMs: Long
    ): List<CameraStream> {
        val streams = mutableListOf<CameraStream>()

        // 先找到所有 Stream 的起始位置
        val streamStartPattern = Regex("""Stream\[(\d+)]:\s*(\w+)""")
        val streamMatches = streamStartPattern.findAll(block).toList()

        for (i in streamMatches.indices) {
            val streamMatch = streamMatches[i]
            val streamId = streamMatch.groupValues[1].toIntOrNull() ?: 0
            val streamType = streamMatch.groupValues[2]

            // 确定当前流块的范围
            val startIndex = streamMatch.range.last + 1
            val endIndex = if (i + 1 < streamMatches.size) {
                streamMatches[i + 1].range.first
            } else {
                block.length
            }

            val streamContent = block.substring(startIndex, endIndex)

            // 解析消费者名称
            val consumerPattern = Regex("""Consumer name:\s*(.+?)\s*\n""")
            val consumerName =
                consumerPattern.find(streamContent)?.groupValues?.get(1)?.trim() ?: ""

            // 解析尺寸和格式
            // Dims: 1280 x 720, format 0x22, dataspace 0x8c20000
            val dimsPattern =
                Regex("""Dims:\s*(\d+)\s*x\s*(\d+),\s*format\s*(0x[\da-fA-F]+),\s*dataspace\s*(0x[\da-fA-F]+)""")
            val dimsMatch = dimsPattern.find(streamContent)

            val width = dimsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val height = dimsMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val format = dimsMatch?.groupValues?.get(3) ?: ""
            val dataSpace = dimsMatch?.groupValues?.get(4) ?: ""

            // 格式名称
            val formatName = PixelFormatHelper.getFormatName(format)

            // 解析旋转角度 (可选)
            val rotationPattern = Regex("""Rotation:\s*(\d+)""")
            val rotation =
                rotationPattern.find(streamContent)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            // 解析用途标志
            val usagePattern = Regex("""Combined usage:\s*(0x[\da-fA-F]+)""")
            val usage = usagePattern.find(streamContent)?.groupValues?.get(1) ?: ""

            // 解析帧统计
            // Frames produced: 1459, last timestamp: 454546233237780 ns
            val framesPattern = Regex("""Frames produced:\s*(\d+),\s*last timestamp:\s*(\d+)""")
            val framesMatch = framesPattern.find(streamContent)
            val framesProduced = framesMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
            val lastTimestamp = framesMatch?.groupValues?.get(2)?.toLongOrNull() ?: 0

            // 计算实时帧率 - 使用设备ID和流ID作为唯一标识
            val streamKey = "device_${deviceId}_stream_$streamId"
            val calculatedFps = calculateFps(streamKey, framesProduced, currentTimeMs)

            streams.add(
                CameraStream(
                    streamId = streamId,
                    type = streamType,
                    consumerName = consumerName,
                    width = width,
                    height = height,
                    format = format,
                    formatName = formatName,
                    dataSpace = dataSpace,
                    rotation = rotation,
                    usage = usage,
                    framesProduced = framesProduced,
                    lastTimestamp = lastTimestamp,
                    calculatedFps = calculatedFps
                )
            )
        }

        return streams
    }

    /**
     * 计算实时帧率
     *
     * 通过比较当前帧数和上次记录的帧数，结合时间间隔计算帧率
     *
     * @param streamKey 流的唯一标识
     * @param currentFrames 当前帧数
     * @param currentTimeMs 当前时间戳（毫秒）
     * @return 计算得出的帧率
     */
    private fun calculateFps(streamKey: String, currentFrames: Long, currentTimeMs: Long): Float {
        val lastRecord = lastFrameCountMap[streamKey]

        return if (lastRecord != null) {
            val (lastFrames, lastTimeMs) = lastRecord
            val frameDelta = currentFrames - lastFrames
            val timeDeltaMs = currentTimeMs - lastTimeMs

            // 更新记录
            lastFrameCountMap[streamKey] = Pair(currentFrames, currentTimeMs)

            if (timeDeltaMs > 0 && frameDelta >= 0) {
                // FPS = 帧数差 / 时间差(秒)
                (frameDelta * 1000f / timeDeltaMs)
            } else {
                0f
            }
        } else {
            // 首次记录
            lastFrameCountMap[streamKey] = Pair(currentFrames, currentTimeMs)
            0f
        }
    }

    /**
     * 解析帧统计信息
     *
     * @param block 设备信息块
     * @param streamList 流列表（用于汇总帧率）
     */
    private fun parseFrameStats(block: String, streamList: List<CameraStream>): CameraFrameStats {
        val statusPattern = Regex("""Device status:\s*(\w+)""")
        val statusMatch = statusPattern.find(block)

        // 从流信息中获取总帧数
        val totalFrames = streamList.sumOf { it.framesProduced }

        // 计算平均帧率（取所有流的平均值）
        val avgFps = if (streamList.isNotEmpty()) {
            streamList.map { it.calculatedFps }.average().toFloat()
        } else {
            0f
        }

        return CameraFrameStats(
            totalFrames = totalFrames,
            currentFps = avgFps,
            deviceStatus = statusMatch?.groupValues?.get(1) ?: ""
        )
    }
}
