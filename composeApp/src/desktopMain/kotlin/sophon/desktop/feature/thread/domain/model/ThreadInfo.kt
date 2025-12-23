package sophon.desktop.feature.thread.domain.model

data class ThreadInfo(
    val user: String = "",
    val pid: String = "",
    val tid: String = "",
    val ppid: String = "",
    val vsz: String = "",
    val rss: String = "",
    val wchan: String = "",
    val address: String = "",
    val state: String = "",
    val cmd: String = "",
)
