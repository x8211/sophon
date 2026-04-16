package sophon.desktop.feature.adb.model

data class AdbState(
    val status: AdbStatus = AdbStatus.Init,
    val adbToolPath: String = "",
    val connectingDevices: List<String> = emptyList(),
    val selectedDevice: String = "",
)

sealed class AdbStatus(open val text: String = "") {
    data object Init : AdbStatus("")
    data class Loading(override val text: String) : AdbStatus(text)
    data object Success : AdbStatus("")
    data class Fail(override val text: String) : AdbStatus(text)
}
