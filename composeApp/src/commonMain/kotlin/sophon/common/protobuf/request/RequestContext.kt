package sophon.common.protobuf.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val DEVICE_INFO_REQ = 1001

@Serializable
class RequestContext(val cmd: Int, val content: String) {

    override fun toString() = Json.encodeToString(this)

    companion object {
        inline fun <reified T> create(cmd: Int, data: T): RequestContext {
            return RequestContext(cmd, Json.encodeToString(data))
        }

        fun parseFrom(string: String): RequestContext = Json.decodeFromString(string)
    }
}

@Serializable
data object DeviceInfoReq {
    fun parseFrom(string: String): DeviceInfoReq = Json.decodeFromString(string)
}