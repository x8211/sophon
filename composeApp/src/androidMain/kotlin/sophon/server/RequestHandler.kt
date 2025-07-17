package sophon.server

import sophon.common.protobuf.request.DEVICE_INFO_REQ
import sophon.common.protobuf.response.ResponseContext
import sophon.server.feature.DeviceInfoReqHandler

interface RequestHandler {
    fun handle(request: String): ResponseContext
}

val requestHandlerMap = mapOf<Int, RequestHandler>(
    DEVICE_INFO_REQ to DeviceInfoReqHandler(),
)
