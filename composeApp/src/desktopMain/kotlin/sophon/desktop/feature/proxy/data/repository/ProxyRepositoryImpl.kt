package sophon.desktop.feature.proxy.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.proxy.domain.repository.ProxyRepository
import java.net.NetworkInterface

class ProxyRepositoryImpl : ProxyRepository {

    override suspend fun getProxy(): String {
        return "adb shell settings get global http_proxy".oneshotShell { it.trim() }
    }

    override suspend fun modifyProxy(proxy: String) {
        // Original logic appended :8888
        "adb shell settings put global http_proxy $proxy:8888".oneshotShell { it }
    }

    override suspend fun resetProxy() {
        "adb shell settings put global http_proxy :0".oneshotShell { it }
    }

    override fun getLocalIPAddresses(): List<String> {
        return NetworkInterface.getNetworkInterfaces()
            .toList()
            .filterNot { it.isLoopback }
            .flatMap { ni -> ni.inetAddresses().filter { it.isSiteLocalAddress }.toList() }
            .map { it.hostAddress }
    }
}
