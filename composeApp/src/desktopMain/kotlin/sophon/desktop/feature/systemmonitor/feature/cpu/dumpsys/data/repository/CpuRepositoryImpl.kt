package sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.systemmonitor.feature.cpu.common.domain.model.ThreadCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.CpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.CpuLoadInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.CpuTimeRange
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.ProcessCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.SystemCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.repository.CpuRepository

/**
 * CPU监测数据仓库实现
 * 通过 ADB Shell 命令获取设备的CPU使用信息
 */
class CpuRepositoryImpl : CpuRepository {

    override suspend fun getCpuData(): CpuData {
        return try {
            // 构建命令
            val command = "adb shell dumpsys cpuinfo"

            // 执行命令并解析输出
            command.oneshotShell { output -> parseCpuInfo(output) }
        } catch (e: Exception) {
            e.printStackTrace()
            CpuData()
        }
    }

    override suspend fun getProcessThreads(pid: Int): List<ThreadCpuInfo> {
        return try {
            // 使用top命令获取进程的线程信息
            // -H: 显示线程
            // -p: 指定进程ID
            // -n 1: 只采样一次
            // -b: 批处理模式
            val command = "adb shell top -H -p $pid -n 1 -b"
            
            command.oneshotShell { output -> parseThreadInfo(output) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 解析CPU信息输出
     */
    private fun parseCpuInfo(output: String): CpuData {
        val lines = output.lines()

        // 解析负载信息 (第一行: Load: 10.05 / 10.1 / 7.69)
        val loadInfo = parseLoadInfo(lines)

        // 解析时间范围 (第二行: CPU usage from 18992ms to 8032ms ago ...)
        val timeRange = parseTimeRange(lines)

        // 解析进程列表和系统CPU信息
        val (processList, systemCpu) = parseProcessList(lines)


        return CpuData(
            loadInfo = loadInfo,
            timeRange = timeRange,
            processList = processList,
            systemCpu = systemCpu
        )
    }

    /**
     * 解析线程信息输出
     * top命令输出示例:
     * Threads: 234 total,   0 running, 234 sleeping,   0 stopped,   0 zombie
     *   Mem:  5596604K total,  5516032K used,    80572K free,     1524K buffers
     *  Swap:  2097148K total,  1711168K used,   385980K free,  1476864K cached
     * 800%cpu  64%user   4%nice  32%sys 693%idle   0%iow   4%irq   4%sirq   0%host
     *   TID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ THREAD          PROCESS
     * 12742 u0_a733      10 -10  43G 558M 257M S 14.2  10.2   0:24.80 RenderThread    com.mico
     * 12811 u0_a733      20   0  43G 558M 257M S 10.7  10.2   0:10.72 DefaultDispatch com.mico
     */
    private fun parseThreadInfo(output: String): List<ThreadCpuInfo> {
        val threads = mutableListOf<ThreadCpuInfo>()
        val lines = output.lines()
        
        // 找到表头行的索引（包含TID, USER, THREAD等关键字）
        val headerIndex = lines.indexOfFirst { 
            it.contains("TID") && it.contains("USER") && it.contains("THREAD") 
        }
        
        if (headerIndex < 0 || headerIndex >= lines.size - 1) {
            return emptyList()
        }
        
        // 从表头的下一行开始解析线程数据
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val threadInfo = parseThreadLine(line)
            if (threadInfo != null) {
                threads.add(threadInfo)
            }
        }
        
        return threads
    }

    /**
     * 解析单行线程信息
     * 示例: 12742 u0_a733      10 -10  43G 558M 257M S 14.2  10.2   0:24.80 RenderThread    com.mico
     * 格式: TID USER PR NI VIRT RES SHR S[%CPU] %MEM TIME+ THREAD PROCESS
     * 索引:  0   1    2  3   4   5   6  7  8     9    10    11     12
     */
    private fun parseThreadLine(line: String): ThreadCpuInfo? {
        return try {
            // 使用空格分割，过滤空字符串
            val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
            
            // 至少需要12个字段才能包含线程名称
            if (parts.size < 12) return null
            
            // TID在第0列
            val tid = parts[0].toIntOrNull() ?: return null
            
            // CPU使用率在第8列
            val cpuPercent = parts[8].toFloatOrNull() ?: 0f
            
            // 线程名称在第11列
            val threadName = parts.getOrNull(11) ?: "Thread-$tid"
            
            // top命令不直接提供用户态和内核态的分离数据
            // 这里简化处理，假设大部分是用户态
            ThreadCpuInfo(
                tid = tid,
                threadName = threadName,
                totalPercent = cpuPercent,
                userPercent = cpuPercent * 0.8f, // 估算80%为用户态
                kernelPercent = cpuPercent * 0.2f // 估算20%为内核态
            )
        } catch (e: Exception) {
            null
        }
    }


    /**
     * 解析负载信息
     * 示例: Load: 10.05 / 10.1 / 7.69
     */
    private fun parseLoadInfo(lines: List<String>): CpuLoadInfo {
        val loadLine = lines.firstOrNull { it.trim().startsWith("Load:") } ?: return CpuLoadInfo()

        return try {
            // 提取负载数值
            val parts =
                loadLine.substringAfter("Load:").trim().split("/").map { it.trim().toFloat() }
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
     * 示例1: CPU usage from 18992ms to 8032ms ago (2026-01-09 17:46:00.000 to 2026-01-09 17:46:10.960):
     * 示例2: CPU usage from 300357ms to 281ms ago (2026-02-02 13:46:15.327 to 2026-02-02 13:51:15.403) with 99% awake:
     */
    private fun parseTimeRange(lines: List<String>): CpuTimeRange {
        val timeLine = lines.firstOrNull { it.contains("CPU usage from") } ?: return CpuTimeRange()

        return try {
            // 提取时间信息
            val durationMatch = Regex("""from (\d+)ms to (\d+)ms ago""").find(timeLine)
            val timeMatch = Regex("""\((.+?) to (.+?)\)""").find(timeLine)

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
            val pattern =
                Regex("""(\d+(?:\.\d+)?)%\s+(\d+)/([^:]+):\s+(\d+(?:\.\d+)?)%\s+user\s+\+\s+(\d+(?:\.\d+)?)%\s+kernel(?:\s+/\s+faults:\s+(\d+)\s+minor(?:\s+(\d+)\s+major)?)?""")
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
            val pattern =
                Regex("""(\d+(?:\.\d+)?)%\s+TOTAL:\s+(\d+(?:\.\d+)?)%\s+user\s+\+\s+(\d+(?:\.\d+)?)%\s+kernel\s+\+\s+(\d+(?:\.\d+)?)%\s+iowait\s+\+\s+(\d+(?:\.\d+)?)%\s+irq\s+\+\s+(\d+(?:\.\d+)?)%\s+softirq""")
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
