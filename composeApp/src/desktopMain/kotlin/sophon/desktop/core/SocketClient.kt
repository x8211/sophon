package sophon.desktop.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sophon.common.protobuf.request.RequestContext
import sophon.common.protobuf.response.ResponseContext
import sophon.desktop.core.Shell.simpleShell
import java.net.Socket

/**
 * Socket连接客户端
 * 负责与Android设备的LocalServerSocket进行连接
 */
object SocketClient {

    private val scope = MainScope()
    private var connectInfo: ConnectInfo? = null

    private val _response = MutableSharedFlow<ResponseContext>()
    val response = _response.asSharedFlow()

    /**
     * 连接到Android设备的LocalServerSocket
     * 使用adb forward命令将本地端口转发到Android设备上的LocalServerSocket
     */
    fun connect(device: String) {
        if (device == connectInfo?.device) return
        connectInfo?.disconnect() //断开之前的连接
        scope.launch {
            try {
                // 设置端口转发
                "adb forward tcp:8888 localabstract:sophon".simpleShell()
                // 连接到本地端口
                connectInfo = ConnectInfo(device, Socket("localhost", 8888))
                println("连接成功")
                connectInfo?.startReceiving()
            } catch (e: Exception) {
                println("连接失败: ${e.message}")
            }
        }
    }

    fun sendData(req: RequestContext) {
        connectInfo?.sendData(req)
    }

    fun disconnect() {
        connectInfo?.disconnect()
    }


    class ConnectInfo(val device: String, val socket: Socket) {

        private val reader = socket.inputStream.bufferedReader()
        private val writer = socket.outputStream.bufferedWriter()

        suspend fun startReceiving() {
            withContext(Dispatchers.IO) {
                innerPrint("开始接收数据: $socket")
                try {
                    while (socket.isConnected) {
                        val data = reader.readLine() ?: return@withContext
                        // 处理接收到的数据
                        innerPrint("接收数据: $data")
                        _response.emit(ResponseContext.parseFrom(data))
                    }
                } catch (e: Exception) {
                    innerPrint("接收失败: ${e.message}")
                }
            }
        }

        /**
         * 发送数据到Android设备
         */
        fun sendData(req: RequestContext) {
            innerPrint("发送数据: $req")
            try {
                if (!socket.isConnected) {
                    innerPrint("发送数据失败：未连接")
                    return
                }
                writer.write(req.toString())
                writer.newLine()
                writer.flush()
            } catch (e: Exception) {
                innerPrint("发送数据失败: ${e.message}")
            }
        }

        /**
         * 断开连接
         */
        fun disconnect() {
            scope.launch {
                innerPrint("开始断开连接: $socket")
                try {
                    socket.close()
                } catch (e: Exception) {
                    innerPrint("disconnect error: ${e.message},ignore")
                } finally {
                    reader.close()
                    writer.close()
                    innerPrint("已断开连接")
                }
                // 移除端口转发
                "adb forward --remove tcp:8888".simpleShell()
            }
        }

        private fun innerPrint(msg: String) =
            println("[$device][${Thread.currentThread().name}]$msg")
    }
}