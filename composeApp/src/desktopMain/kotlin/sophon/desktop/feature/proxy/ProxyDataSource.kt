package sophon.desktop.feature.proxy

import sophon.desktop.core.Shell.oneshotShell

class ProxyDataSource {

    suspend fun getProxy(): String {
        return "adb shell settings get global http_proxy".oneshotShell { it }
    }

    suspend fun modifyProxy(proxy: String) {
        "adb shell settings put global http_proxy $proxy:8888".oneshotShell { it }
    }

    suspend fun resetProxy() {
        "adb shell settings put global http_proxy :0".oneshotShell { it }
    }

}