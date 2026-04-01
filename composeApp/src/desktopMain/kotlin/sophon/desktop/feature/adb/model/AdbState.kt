package sophon.desktop.feature.adb.model

import java.io.File

data class AdbState(
    val status: AdbStatus = AdbStatus.Init,
    val adbToolPath: String = "",
    val connectingDevices: List<String> = emptyList(),
    val selectedDevice: String = "",
) {
    val adbParentPath: String? by lazy { File(adbToolPath).parent }
}

sealed class AdbStatus(open val text: String = "") {
    data object Init : AdbStatus("")
    data class Loading(override val text: String) : AdbStatus(text)
    data object Success : AdbStatus("")
    data class Fail(override val text: String) : AdbStatus(text)
}
