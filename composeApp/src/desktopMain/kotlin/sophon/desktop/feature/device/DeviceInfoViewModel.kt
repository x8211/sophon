package sophon.desktop.feature.device

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

class DeviceInfoViewModel : StateScreenModel<List<DeviceInfoSection>>(emptyList()) {

    init {
        screenModelScope.launch {
            mutableState.value = parseGetProp(getProp())
        }
    }
}

data class DeviceInfoSection(
    val name: String,
    val items: MutableList<DeviceInfoItem> = mutableListOf()
) {
    fun addItem(key: String, value: String) {
        items.add(DeviceInfoItem(key, value))
    }
}

data class DeviceInfoItem(val name: String, val value: String)