package sophon.server

import android.net.LocalSocket
import sophon.common.protobuf.request.RequestContext

class Connection(private val localSocket: LocalSocket) : Thread() {

    private val reader = localSocket.inputStream.bufferedReader()
    private val writer = localSocket.outputStream.bufferedWriter()

    override fun run() {
        try {
            while (!isInterrupted && localSocket.isConnected) {
                val message = reader.readLine() ?: break // 当连接断开时返回null
                val requestContext = RequestContext.parseFrom(message)
                "收到消息: ${requestContext.cmd} (来自 ${localSocket.localSocketAddress})".logI()
                val response = requestHandlerMap[requestContext.cmd]?.handle(requestContext.content)
                "回复消息: $response 给 ${localSocket.localSocketAddress}".logI()
                writer.write(response.toString())
                writer.newLine()
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