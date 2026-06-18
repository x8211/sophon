package sophon.desktop.feature.packetcapture.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.packetcapture.data.source.MitmProxyDataSource
import sophon.desktop.feature.packetcapture.data.source.cert.CertificateAuthority
import sophon.desktop.feature.packetcapture.model.CapturedPacket
import sophon.desktop.feature.packetcapture.model.ThrottleConfig
import sophon.desktop.feature.proxy.data.repository.ProxyRepository
import sophon.desktop.feature.proxy.data.repository.ProxyRepositoryImpl

/**
 * [PacketCaptureRepository] 的具体实现，负责 [MitmProxyServer] 的生命周期管理，
 * 并通过 [callbackFlow] 将 Netty 的抓包回调桥接为 Kotlin 协程 Flow。
 */
class PacketCaptureRepositoryImpl(
    private val proxyRepository: ProxyRepository = ProxyRepositoryImpl()
) : PacketCaptureRepository {

    private var proxyServer: MitmProxyDataSource? = null
    private var pendingThrottleConfig: ThrottleConfig = ThrottleConfig()

    override fun startCapture(port: Int): Flow<CapturedPacket> = callbackFlow {
        val server = MitmProxyDataSource { packet ->
            trySend(packet)
        }
        proxyServer = server

        val result = server.start(port)
        if (result.isFailure) {
            close(result.exceptionOrNull() ?: Exception("代理服务器启动失败"))
            return@callbackFlow
        }

        server.updateThrottle(pendingThrottleConfig)

        awaitClose {
            server.stop()
            proxyServer = null
        }
    }

    override fun stopCapture() {
        proxyServer?.stop()
        proxyServer = null
    }

    override suspend fun getDeviceProxy(): String = proxyRepository.getProxy()

    override suspend fun installCaToDevice() {
        val caCertPath = CertificateAuthority.getCaCertFile().absolutePath
        "adb push $caCertPath /sdcard/MicoToolboxCA.crt".simpleShell()
    }

    override fun getCaCertPath(): String = CertificateAuthority.getCaCertFile().absolutePath

    override fun updateThrottle(config: ThrottleConfig) {
        pendingThrottleConfig = config
        proxyServer?.updateThrottle(config)
    }
}
