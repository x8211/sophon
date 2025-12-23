package sophon.desktop.feature.adb.domain.model

/**
 * ADB 状态模型
 */
data class AdbState(
    val status: AdbStatus = AdbStatus.Init,
    val adbToolPath: String = "", // adb 工具路径
    val adbToolAvailable: Boolean = false, // adb 工具是否可用
    val connectingDevices: List<String> = emptyList(), // 正在连接的 adb 设备
    val selectedDevice: String = "", // 选中连接的 adb 设备
    val adbParentPath: String? = null // adb 所在的父目录
)

/**
 * ADB 加载状态
 */
sealed class AdbStatus(open val text: String = "") {
    data object Init : AdbStatus("")
    data class Loading(override val text: String) : AdbStatus(text)
    data object Success : AdbStatus("")
    data class Fail(override val text: String) : AdbStatus(text)
}
