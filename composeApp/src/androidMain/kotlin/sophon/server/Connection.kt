package sophon.server

import android.net.LocalSocket
import sophon.server.feature.PackageManager

class Connection(private val localSocket: LocalSocket) : Thread() {

    private val reader = localSocket.inputStream.bufferedReader()
    private val writer = localSocket.outputStream.bufferedWriter()

    override fun run() {
        try {
            while (!isInterrupted && localSocket.isConnected) {
                val message = reader.readLine() ?: break // 当连接断开时返回null
                "收到消息: $message (来自 ${localSocket.localSocketAddress})".logI()
                val resp = when (message) {
                    "hello" -> "world"
                    "queryPackageInfo" -> PackageManager.getPackageInfo("com.mico")?:"queryPackagerInfo fail"
                    else -> "unknown"
                }
                "回复消息: $resp 给 ${localSocket.localSocketAddress}".logI()
                writer.write("$resp\n")
                writer.flush()
            }
        } catch (e: Exception) {
            "连接异常".logE(e)
        } finally {
            localSocket.close()
            reader.close()
            writer.close()
            "连接已关闭".logI()
        }
    }

}