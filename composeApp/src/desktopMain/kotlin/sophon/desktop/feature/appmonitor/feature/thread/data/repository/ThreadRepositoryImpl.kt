package sophon.desktop.feature.appmonitor.feature.thread.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.appmonitor.feature.thread.domain.model.ProcessInfo
import sophon.desktop.feature.appmonitor.feature.thread.domain.model.ThreadInfo
import sophon.desktop.feature.appmonitor.feature.thread.domain.repository.ThreadRepository

/**
 * 线程信息仓库实现
 * 
 * 通过ADB命令获取进程和线程信息
 */
class ThreadRepositoryImpl : ThreadRepository {

    override suspend fun getPidByPackageName(packageName: String): String {
        return "adb shell pidof $packageName".simpleShell()
    }

    override suspend fun getThreadList(pid: String, packageName: String): ProcessInfo? {
        return "adb shell ps -T -p $pid".oneshotShell { str ->
            val lines = str.split("\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) return@oneshotShell null

            // 解析所有行
            val parsedLines = lines.map { line ->
                val data = line.replace(Regex("\\s+"), "%").split("%", limit = 10)
                data
            }

            // 第一行是标题行,跳过
            if (parsedLines.size < 2) return@oneshotShell null
            
            // 从第二行开始是实际数据
            val dataLines = parsedLines.drop(1)
            
            // 提取进程级别的信息(从第一个数据行获取)
            val firstDataLine = dataLines.firstOrNull()
            if (firstDataLine == null || firstDataLine.size < 10) return@oneshotShell null
            
            val processUser = firstDataLine[0]
            val processPid = firstDataLine[1]
            val processPpid = firstDataLine[3]
            val processVsz = firstDataLine[4]
            val processRss = firstDataLine[5]
            
            // 解析所有线程信息
            val threads = dataLines.mapNotNull { data ->
                if (data.size < 10) {
                    null
                } else {
                    ThreadInfo(
                        tid = data[2],
                        wchan = data[6],
                        address = data[7],
                        state = data[8],
                        cmd = data[9].replace("%", " ")
                    )
                }
            }
            
            // 按状态排序:运行中的线程排在前面
            val sortedThreads = threads.sortedByDescending { it.state == "R" }
            
            ProcessInfo(
                user = processUser,
                pid = processPid,
                ppid = processPpid,
                vsz = processVsz,
                rss = processRss,
                packageName = packageName,
                threads = sortedThreads
            )
        }
    }
}
