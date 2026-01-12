package sophon.desktop.feature.cpumonitor.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.cpumonitor.domain.model.CpuLoadInfo
import sophon.desktop.feature.cpumonitor.domain.model.CpuMonitorData
import sophon.desktop.feature.cpumonitor.domain.model.CpuTimeRange
import sophon.desktop.feature.cpumonitor.domain.model.ProcessCpuInfo
import sophon.desktop.feature.cpumonitor.domain.model.SystemCpuInfo
import sophon.desktop.feature.cpumonitor.domain.repository.CpuMonitorRepository

/**
 * CPU监测数据仓库实现
 * 通过 ADB Shell 命令获取设备的CPU使用信息
 */
class CpuMonitorRepositoryImpl : CpuMonitorRepository {

    override suspend fun getCpuMonitorData(packageName: String?): CpuMonitorData {
        return try {
            // 构建命令
            val command = if (packageName != null) {
                "adb shell dumpsys cpuinfo $packageName"
            } else {
                "adb shell dumpsys cpuinfo"
            }
            
            // 执行命令并解析输出
            command.oneshotShell { output ->
                parseCpuInfo(output, packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CpuMonitorData()
        }
    }

    /**
     * 解析CPU信息输出
     */
    private fun parseCpuInfo(output: String, targetPackage: String?): CpuMonitorData {
        val lines = output.lines()
        
        // 解析负载信息 (第一行: Load: 10.05 / 10.1 / 7.69)
        val loadInfo = parseLoadInfo(lines)
        
        // 解析时间范围 (第二行: CPU usage from 18992ms to 8032ms ago ...)
        val timeRange = parseTimeRange(lines)
        
        // 解析进程列表和系统CPU信息
        val (processList, systemCpu) = parseProcessList(lines)
        
        // 查找目标进程
        val targetProcess = if (targetPackage != null) {
            processList.find { it.processName.contains(targetPackage) }
        } else {
            null
        }
        
        return CpuMonitorData(
            loadInfo = loadInfo,
            timeRange = timeRange,
            targetProcess = targetProcess,
            processList = processList,
            systemCpu = systemCpu
        )
    }

    /**
     * 解析负载信息
     * 示例: Load: 10.05 / 10.1 / 7.69
     */
    private fun parseLoadInfo(lines: List<String>): CpuLoadInfo {
        val loadLine = lines.firstOrNull { it.trim().startsWith("Load:") } ?: return CpuLoadInfo()
        
        return try {
            // 提取负载数值
            val parts = loadLine.substringAfter("Load:").trim().split("/").map { it.trim().toFloat() }
            CpuLoadInfo(
                load1min = parts.getOrNull(0) ?: 0f,
                load5min = parts.getOrNull(1) ?: 0f,
                load15min = parts.getOrNull(2) ?: 0f
            )
        } catch (e: Exception) {
            CpuLoadInfo()
        }
    }

    /**
     * 解析时间范围
     * 示例: CPU usage from 18992ms to 8032ms ago (2026-01-09 17:46:00.000 to 2026-01-09 17:46:10.960):
     */
    private fun parseTimeRange(lines: List<String>): CpuTimeRange {
        val timeLine = lines.firstOrNull { it.contains("CPU usage from") } ?: return CpuTimeRange()
        
        return try {
            // 提取时间信息
            val durationMatch = Regex("""from (\d+)ms to (\d+)ms ago""").find(timeLine)
            val timeMatch = Regex("""\((.+?) to (.+?)\):""").find(timeLine)
            
            val startMs = durationMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
            val endMs = durationMatch?.groupValues?.get(2)?.toLongOrNull() ?: 0
            val startTime = timeMatch?.groupValues?.get(1)?.trim() ?: ""
            val endTime = timeMatch?.groupValues?.get(2)?.trim() ?: ""
            
            CpuTimeRange(
                startTime = startTime,
                endTime = endTime,
                durationMs = startMs - endMs
            )
        } catch (e: Exception) {
            CpuTimeRange()
        }
    }

    /**
     * 解析进程列表和系统CPU信息
     */
    private fun parseProcessList(lines: List<String>): Pair<List<ProcessCpuInfo>, SystemCpuInfo> {
        val processList = mutableListOf<ProcessCpuInfo>()
        var systemCpu = SystemCpuInfo()
        
        // 找到进程列表开始的位置 (在时间范围行之后)
        val startIndex = lines.indexOfFirst { it.contains("CPU usage from") } + 1
        if (startIndex <= 0 || startIndex >= lines.size) {
            return Pair(emptyList(), SystemCpuInfo())
        }
        
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            
            // 跳过空行
            if (line.isEmpty()) continue
            
            // 解析系统总体CPU (最后一行: 62% TOTAL: 36% user + 21% kernel + 0% iowait + 3.2% irq + 1.3% softirq)
            if (line.contains("TOTAL:")) {
                systemCpu = parseSystemCpu(line)
                break
            }
            
            // 解析进程CPU信息
            // 示例: 140% 4521/com.mico: 110% user + 30% kernel / faults: 19894 minor
            val processInfo = parseProcessCpu(line)
            if (processInfo != null) {
                processList.add(processInfo)
            }
        }
        
        return Pair(processList, systemCpu)
    }

    /**
     * 解析单个进程的CPU信息
     * 示例: 140% 4521/com.mico: 110% user + 30% kernel / faults: 19894 minor
     * 示例: 136% 1082/android.hardware.camera.provider@2.7-service-google: 107% user + 28% kernel / faults: 305 minor 4 major
     */
    private fun parseProcessCpu(line: String): ProcessCpuInfo? {
        return try {
            // 匹配格式: <total%> <pid>/<name>: <user%> user + <kernel%> kernel [/ faults: <minor> minor [<major> major]]
            val pattern = Regex("""(\d+(?:\.\d+)?)%\s+(\d+)/([^:]+):\s+(\d+(?:\.\d+)?)%\s+user\s+\+\s+(\d+(?:\.\d+)?)%\s+kernel(?:\s+/\s+faults:\s+(\d+)\s+minor(?:\s+(\d+)\s+major)?)?""")
            val match = pattern.find(line) ?: return null
            
            val totalPercent = match.groupValues[1].toFloatOrNull() ?: 0f
            val pid = match.groupValues[2].toIntOrNull() ?: 0
            val processName = match.groupValues[3].trim()
            val userPercent = match.groupValues[4].toFloatOrNull() ?: 0f
            val kernelPercent = match.groupValues[5].toFloatOrNull() ?: 0f
            val minorFaults = match.groupValues.getOrNull(6)?.toIntOrNull() ?: 0
            val majorFaults = match.groupValues.getOrNull(7)?.toIntOrNull() ?: 0
            
            ProcessCpuInfo(
                pid = pid,
                processName = processName,
                totalPercent = totalPercent,
                userPercent = userPercent,
                kernelPercent = kernelPercent,
                minorFaults = minorFaults,
                majorFaults = majorFaults
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析系统整体CPU信息
     * 示例: 62% TOTAL: 36% user + 21% kernel + 0% iowait + 3.2% irq + 1.3% softirq
     */
    private fun parseSystemCpu(line: String): SystemCpuInfo {
        return try {
            val pattern = Regex("""(\d+(?:\.\d+)?)%\s+TOTAL:\s+(\d+(?:\.\d+)?)%\s+user\s+\+\s+(\d+(?:\.\d+)?)%\s+kernel\s+\+\s+(\d+(?:\.\d+)?)%\s+iowait\s+\+\s+(\d+(?:\.\d+)?)%\s+irq\s+\+\s+(\d+(?:\.\d+)?)%\s+softirq""")
            val match = pattern.find(line) ?: return SystemCpuInfo()
            
            SystemCpuInfo(
                totalPercent = match.groupValues[1].toFloatOrNull() ?: 0f,
                userPercent = match.groupValues[2].toFloatOrNull() ?: 0f,
                kernelPercent = match.groupValues[3].toFloatOrNull() ?: 0f,
                iowaitPercent = match.groupValues[4].toFloatOrNull() ?: 0f,
                irqPercent = match.groupValues[5].toFloatOrNull() ?: 0f,
                softirqPercent = match.groupValues[6].toFloatOrNull() ?: 0f
            )
        } catch (e: Exception) {
            SystemCpuInfo()
        }
    }
}
