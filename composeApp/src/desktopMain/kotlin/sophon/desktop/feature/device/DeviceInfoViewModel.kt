package sophon.desktop.feature.device

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import sophon.common.protobuf.request.DEVICE_INFO_REQ
import sophon.common.protobuf.request.DeviceInfoReq
import sophon.common.protobuf.request.RequestContext
import sophon.common.protobuf.response.DEVICE_INFO_RSP
import sophon.common.protobuf.response.DeviceInfoRsp
import sophon.desktop.core.SocketClient

class DeviceInfoViewModel : StateScreenModel<DeviceInfo>(DeviceInfo()) {

    init {
        screenModelScope.launch {
            SocketClient.response.collect {
                if (it.cmd == DEVICE_INFO_RSP) {
                    val deviceInfo = DeviceInfoRsp.parseFrom(it.content)
                    // 将DeviceInfoRsp转换为DeviceInfo格式
                    val androidSections = deviceInfo.sections.map { section ->
                        section.title to section.items.map { item ->
                            item.key to item.value
                        }
                    }
                    // 更新状态，添加从Android设备获取的信息
                    mutableState.value = mutableState.value.copy(
                        sections = mutableState.value.sections + androidSections
                    )
                }
            }
        }

        SocketClient.sendData(RequestContext.create(DEVICE_INFO_REQ, DeviceInfoReq))
    }
}

data class DeviceInfo(
    val sections: List<Pair<String, List<Pair<String, String>>>> = emptyList(),
)
