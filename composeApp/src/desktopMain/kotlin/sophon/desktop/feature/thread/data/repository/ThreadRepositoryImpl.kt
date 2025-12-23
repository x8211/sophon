package sophon.desktop.feature.thread.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.thread.domain.model.ThreadInfo
import sophon.desktop.feature.thread.domain.repository.ThreadRepository

class ThreadRepositoryImpl : ThreadRepository {

    override suspend fun getPidByPackageName(packageName: String): String {
        return "adb shell pidof $packageName".simpleShell()
    }

    override suspend fun getThreadList(pid: String): List<ThreadInfo> {
        return "adb shell ps -T -p $pid".oneshotShell { str ->
            val lines = str.split("\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) return@oneshotShell emptyList()

            val original = lines.map { line ->
                val data = line.replace(Regex("\\s+"), "%").split("%", limit = 10)
                if (data.size < 10) {
                   // Fallback or padding if line is malformed
                   ThreadInfo(cmd = line)
                } else {
                    ThreadInfo(
                        user = data[0],
                        pid = data[1],
                        tid = data[2],
                        ppid = data[3],
                        vsz = data[4],
                        rss = data[5],
                        wchan = data[6],
                        address = data[7],
                        state = data[8],
                        cmd = data[9].replace("%", " ")
                    )
                }
            }

            //第一行是标题行
            val title = original.firstOrNull() ?: return@oneshotShell emptyList()
            //去掉标题行后进行排序，再把标题行填充到第一位
            val list = original
                .takeLast(original.size - 1)
                .sortedByDescending { it.state == "R" }
                .toMutableList()
            list.add(0, title)
            list
        }
    }

    override suspend fun getForegroundPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }
}
