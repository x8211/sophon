package sophon.common.protobuf.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val DEVICE_INFO_RSP = 1001

@Serializable
class ResponseContext(val cmd: Int, val content: String) {

    override fun toString() = Json.encodeToString(this)

    companion object {
        inline fun <reified T> create(cmd: Int, data: T): ResponseContext {
            return ResponseContext(cmd, Json.encodeToString(data))
        }

        fun parseFrom(string: String): ResponseContext = Json.decodeFromString(string)
    }
}

@Serializable
data class DeviceInfoRsp(val sections: List<DeviceInfoSection>) {
    companion object Companion {
        fun parseFrom(string: String): DeviceInfoRsp = Json.decodeFromString(string)
    }
}

@Serializable
data class DeviceInfoSection(val title: String, val items: List<DeviceInfoItem>)

@Serializable
data class DeviceInfoItem(val key: String, val value: String)