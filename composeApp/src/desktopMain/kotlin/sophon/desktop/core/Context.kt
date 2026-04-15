package sophon.desktop.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sophon.desktop.feature.adb.data.repository.AdbRepositoryImpl
import sophon.desktop.feature.adb.model.AdbState

object Context {

    private val scope = MainScope()
    private val adbRepository = AdbRepositoryImpl()

    val stream: StateFlow<AdbState> = adbRepository.getAdbState()

    init {
        scope.launch {
            val adbPath = adbRepository.autoFindAdbTool()
            adbRepository.updateAdbPath(adbPath)
            while (true) {
                adbRepository.refreshDevices()
                delay(3000)
            }
        }
    }

    fun selectDevice(deviceName: String) {
        scope.launch {
            adbRepository.selectDevice(deviceName)
        }
    }
}
