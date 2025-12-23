package sophon.desktop.core.datastore

import kotlinx.serialization.Serializable

@Serializable
data class AdbConfig(val toolPath: String = "")

val adbDataStore = createDataStore(
    fileName = "adb.pb",
    defaultValue = AdbConfig(),
    serializer = AdbConfig.serializer()
)
