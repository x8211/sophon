package sophon.desktop.feature.systemmonitor.feature.cpu.realtime.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.systemmonitor.feature.cpu.common.model.ThreadCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model.RealtimeCpuData
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model.RealtimeMemoryInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model.RealtimeProcessInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model.RealtimeSwapInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model.RealtimeSystemCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.model.RealtimeTaskStats
import java.util.Locale

/**
 * 实时CPU监测数据仓库实现
 * 通过 ADB Shell top 命令获取设备的实时CPU使用信息
 */
class RealtimeCpuRepositoryImpl : RealtimeCpuRepository {

    override suspend fun getRealtimeCpuData(): RealtimeCpuData {
        return try {
            // 使用top命令获取实时数据
            // -n 1: 只采样一次
            // -b: 批处理模式
            // -m 10: 最多显示10个进程
            val command = "adb shell top -n 1 -b -m 10"

            command.oneshotShell { output -> parseTopOutput(output) }
        } catch (e: Exception) {
            e.printStackTrace()
            RealtimeCpuData()
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
     * 解析top命令输出
     * 示例输出:
     * Tasks: 747 total,   1 running, 746 sleeping,   0 stopped,   0 zombie
     *   Mem:  7628056K total,  6818428K used,   809628K free,     6836K buffers
     *  Swap:  3145724K total,  1143040K used,  2002684K free,  3854764K cached
     * 800%cpu   4%user   0%nice  21%sys 775%idle   0%iow   0%irq   0%sirq   0%host
     *   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
     * 11491 shell        20   0  10G 5.3M 4.0M R 10.7   0.0   0:00.04 top -n 1 -b -m 10
     */
    private fun parseTopOutput(output: String): RealtimeCpuData {
        val lines = output.lines()

        // 解析任务统计
        val taskStats = parseTaskStats(lines)

        // 解析内存信息
        val memoryInfo = parseMemoryInfo(lines)

        // 解析Swap信息
        val swapInfo = parseSwapInfo(lines)

        // 解析系统CPU信息
        val systemCpu = parseSystemCpuInfo(lines)

        // 解析进程列表
        val processList = parseProcessList(lines)

        return RealtimeCpuData(
            taskStats = taskStats,
            memoryInfo = memoryInfo,
            swapInfo = swapInfo,
            systemCpu = systemCpu,
            processList = processList
        )
    }

    /**
     * 解析任务统计信息
     * 示例: Tasks: 747 total,   1 running, 746 sleeping,   0 stopped,   0 zombie
     */
    private fun parseTaskStats(lines: List<String>): RealtimeTaskStats {
        val taskLine =
            lines.firstOrNull { it.trim().startsWith("Tasks:") } ?: return RealtimeTaskStats()

        return try {
            val pattern =
                Regex("""Tasks:\s+(\d+)\s+total,\s+(\d+)\s+running,\s+(\d+)\s+sleeping,\s+(\d+)\s+stopped,\s+(\d+)\s+zombie""")
            val match = pattern.find(taskLine) ?: return RealtimeTaskStats()

            RealtimeTaskStats(
                total = match.groupValues[1].toIntOrNull() ?: 0,
                running = match.groupValues[2].toIntOrNull() ?: 0,
                sleeping = match.groupValues[3].toIntOrNull() ?: 0,
                stopped = match.groupValues[4].toIntOrNull() ?: 0,
                zombie = match.groupValues[5].toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            RealtimeTaskStats()
        }
    }

    /**
     * 解析内存信息
     * 示例: Mem:  7628056K total,  6818428K used,   809628K free,     6836K buffers
     */
    private fun parseMemoryInfo(lines: List<String>): RealtimeMemoryInfo {
        val memLine =
            lines.firstOrNull { it.trim().startsWith("Mem:") } ?: return RealtimeMemoryInfo()

        return try {
            val pattern =
                Regex("""Mem:\s+(\S+)\s+total,\s+(\S+)\s+used,\s+(\S+)\s+free,\s+(\S+)\s+buffers""")
            val match = pattern.find(memLine) ?: return RealtimeMemoryInfo()

            RealtimeMemoryInfo(
                total = formatMemorySize(match.groupValues[1]),
                used = formatMemorySize(match.groupValues[2]),
                free = formatMemorySize(match.groupValues[3]),
                buffers = formatMemorySize(match.groupValues[4])
            )
        } catch (e: Exception) {
            RealtimeMemoryInfo()
        }
    }

    /**
     * 解析Swap信息
     * 示例: Swap:  3145724K total,  1143040K used,  2002684K free,  3854764K cached
     */
    private fun parseSwapInfo(lines: List<String>): RealtimeSwapInfo {
        val swapLine =
            lines.firstOrNull { it.trim().startsWith("Swap:") } ?: return RealtimeSwapInfo()

        return try {
            val pattern =
                Regex("""Swap:\s+(\S+)\s+total,\s+(\S+)\s+used,\s+(\S+)\s+free,\s+(\S+)\s+cached""")
            val match = pattern.find(swapLine) ?: return RealtimeSwapInfo()

            RealtimeSwapInfo(
                total = formatMemorySize(match.groupValues[1]),
                used = formatMemorySize(match.groupValues[2]),
                free = formatMemorySize(match.groupValues[3]),
                cached = formatMemorySize(match.groupValues[4])
            )
        } catch (e: Exception) {
            RealtimeSwapInfo()
        }
    }

    /**
     * 解析系统CPU信息
     * 示例: 800%cpu   4%user   0%nice  21%sys 775%idle   0%iow   0%irq   0%sirq   0%host
     */
    private fun parseSystemCpuInfo(lines: List<String>): RealtimeSystemCpuInfo {
        val cpuLine = lines.firstOrNull { it.contains("%cpu") } ?: return RealtimeSystemCpuInfo()

        return try {
            val pattern =
                Regex("""(\d+)%cpu\s+(\d+)%user\s+(\d+)%nice\s+(\d+)%sys\s+(\d+)%idle\s+(\d+)%iow\s+(\d+)%irq\s+(\d+)%sirq\s+(\d+)%host""")
            val match = pattern.find(cpuLine) ?: return RealtimeSystemCpuInfo()

            RealtimeSystemCpuInfo(
                totalCpu = match.groupValues[1].toFloatOrNull() ?: 0f,
                userPercent = match.groupValues[2].toFloatOrNull() ?: 0f,
                nicePercent = match.groupValues[3].toFloatOrNull() ?: 0f,
                sysPercent = match.groupValues[4].toFloatOrNull() ?: 0f,
                idlePercent = match.groupValues[5].toFloatOrNull() ?: 0f,
                iowaitPercent = match.groupValues[6].toFloatOrNull() ?: 0f,
                irqPercent = match.groupValues[7].toFloatOrNull() ?: 0f,
                softirqPercent = match.groupValues[8].toFloatOrNull() ?: 0f,
                hostPercent = match.groupValues[9].toFloatOrNull() ?: 0f
            )
        } catch (e: Exception) {
            RealtimeSystemCpuInfo()
        }
    }

    /**
     * 解析进程列表
     * 示例:
     *   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
     * 11491 shell        20   0  10G 5.3M 4.0M R 10.7   0.0   0:00.04 top -n 1 -b -m 10
     */
    private fun parseProcessList(lines: List<String>): List<RealtimeProcessInfo> {
        val processList = mutableListOf<RealtimeProcessInfo>()

        // 找到表头行的索引
        val headerIndex = lines.indexOfFirst {
            it.contains("PID") && it.contains("USER") && it.contains("ARGS")
        }

        if (headerIndex < 0 || headerIndex >= lines.size - 1) {
            return emptyList()
        }

        // 从表头的下一行开始解析进程数据
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val processInfo = parseProcessLine(line)
            if (processInfo != null) {
                processList.add(processInfo)
            }
        }

        return processList
    }

    /**
     * 解析单行进程信息
     * 示例: 11491 shell        20   0  10G 5.3M 4.0M R 10.7   0.0   0:00.04 top -n 1 -b -m 10
     * 格式: PID USER PR NI VIRT RES SHR S[%CPU] %MEM TIME+ ARGS
     * 索引:  0   1   2  3   4   5   6  7  8     9    10    11...
     */
    private fun parseProcessLine(line: String): RealtimeProcessInfo? {
        return try {
            // 使用空格分割，过滤空字符串
            val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }

            // 至少需要12个字段
            if (parts.size < 12) return null

            val pid = parts[0].toIntOrNull() ?: return null
            val user = parts[1]
            val priority = parts[2].toIntOrNull() ?: 0
            val niceValue = parts[3].toIntOrNull() ?: 0
            val virtualMemory = parts[4]
            val residentMemory = parts[5]
            val sharedMemory = parts[6]
            val status = parts[7]
            val cpuPercent = parts[8].toFloatOrNull() ?: 0f
            val memPercent = parts[9].toFloatOrNull() ?: 0f
            val time = parts[10]
            // 进程名称可能包含空格，取剩余所有部分
            val processName = parts.drop(11).joinToString(" ")

            RealtimeProcessInfo(
                pid = pid,
                user = user,
                priority = priority,
                niceValue = niceValue,
                virtualMemory = virtualMemory,
                residentMemory = residentMemory,
                sharedMemory = sharedMemory,
                status = status,
                cpuPercent = cpuPercent,
                memPercent = memPercent,
                time = time,
                processName = processName
            )
        } catch (e: Exception) {
            null
        }
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
     * 索引:  0   1   2  3   4   5   6  7  8     9    10    11     12...
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
     * 将字节数格式化为可读的内存大小字符串
     */
    private fun formatMemorySize(sizeStr: String): String {

        /**
         * 解析内存大小字符串为字节数
         * 支持 K, M, G, T, P 单位
         */
        fun parseMemorySize(sizeStr: String): Long {
            val regex = Regex("""(\d+)([KMGTP])?""")
            val match = regex.find(sizeStr.uppercase()) ?: return sizeStr.toLongOrNull() ?: 0L

            val value = match.groupValues[1].toLongOrNull() ?: 0L
            val unit = match.groupValues[2].ifEmpty { "B" }

            return when (unit) {
                "K" -> value * UNIT_K
                "M" -> value * UNIT_M
                "G" -> value * UNIT_G
                "T" -> value * UNIT_T
                "P" -> value * UNIT_P
                else -> value
            }
        }

        val bytes = parseMemorySize(sizeStr)
        return when {
            bytes >= UNIT_P -> "%.2f P".format(Locale.getDefault(), bytes.toDouble() / UNIT_P)
            bytes >= UNIT_T -> "%.2f T".format(Locale.getDefault(), bytes.toDouble() / UNIT_T)
            bytes >= UNIT_G -> "%.2f G".format(Locale.getDefault(), bytes.toDouble() / UNIT_G)
            bytes >= UNIT_M -> "%.2f M".format(Locale.getDefault(), bytes.toDouble() / UNIT_M)
            bytes >= UNIT_K -> "%.2f K".format(Locale.getDefault(), bytes.toDouble() / UNIT_K)
            else -> "${bytes}B"
        }
    }

    companion object {
        private const val UNIT_P = 1024L * 1024L * 1024L * 1024L * 1024L
        private const val UNIT_T = 1024L * 1024L * 1024L * 1024L
        private const val UNIT_G = 1024L * 1024L * 1024L
        private const val UNIT_M = 1024L * 1024L
        private const val UNIT_K = 1024L
    }
}
