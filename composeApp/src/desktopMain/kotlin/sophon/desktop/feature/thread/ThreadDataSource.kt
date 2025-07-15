package sophon.desktop.feature.thread

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell

class ThreadDataSource {

    /**
     * 由包名获取 pid
     */
    suspend fun queryPidWithPkg(packageName: String): String {
        return "adb shell pidof $packageName".simpleShell()
    }

    suspend fun queryThreadList(pid: String): List<ThreadInfo> {
        return "adb shell ps -T -p $pid".oneshotShell { str ->
            val original = str.split("\n").filter { it.isNotBlank() }.map {
                val data = it.replace(Regex("\\s+"), "%").split("%", limit = 10)
                ThreadInfo(
                    data[0], data[1], data[2], data[3], data[4],
                    data[5], data[6], data[7], data[8], data[9].replace("%", " ")
                )
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
}

data class ThreadInfo(
    val user: String = "",
    val pid: String = "",
    val tid: String = "",
    val pPid: String = "",
    val vsz: String = "",
    val rss: String = "",
    val wChan: String = "",
    val address: String = "",
    val state: String = "",
    val cmd: String = "",
)