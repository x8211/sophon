package sophon.server

import android.net.LocalServerSocket
import java.util.concurrent.Executors
import kotlin.system.exitProcess

class Server {

    private val executor = Executors.newCachedThreadPool()
    private var server: LocalServerSocket? = null

    fun start() {
        try {
            "server starting".logI()
            server = LocalServerSocket("sophon")
            while (true) {
                server?.apply {
                    "server started: listening on ${localSocketAddress.name}".logI()
                    val localSocket = accept()
                    "client connected: ${localSocket.localSocketAddress}".logI()
                    executor.submit(Connection(localSocket)).get()
                    return@apply
                }
                break
            }
        } catch (e: Exception) {
            "start server error".logE(e)
        } finally {
            "server exit".logI()
            server?.close()
            exitProcess(0)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Server().start()
        }
    }
}