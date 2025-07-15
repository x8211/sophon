package sophon.desktop.core

import sophon.desktop.core.Shell.simpleShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

/**
 * Sophon Socket连接仓库
 * 负责与Android设备的LocalServerSocket进行连接
 */
object SophonSocketRepository {
    private val scope = MainScope()

    private var socket: Socket? = null
    private var inputReader: BufferedReader? = null
    private var outputWriter: BufferedWriter? = null

    /**
     * 连接到Android设备的LocalServerSocket
     * 使用adb forward命令将本地端口转发到Android设备上的LocalServerSocket
     */
    fun connect() {
        scope.launch {
            try {
                // 设置端口转发
                "adb forward tcp:8888 localabstract:sophon".simpleShell()

                // 连接到本地端口
                Socket("localhost", 8888).let {
                    socket = it
                    outputWriter = DataOutputStream(it.getOutputStream()).bufferedWriter()
                    DataInputStream(it.getInputStream()).let { inStream ->
                        inputReader = inStream.bufferedReader()
                        println("连接成功")
                        startReceiving(it, inputReader!!)
                    }
                }
            } catch (e: Exception) {
                println("连接失败: ${e.message}")
                disconnect()
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        scope.launch {
            println("开始断开连接")
            try {
                socket?.close()
            } catch (e: Exception) {
                println("disconnect error: ${e.message},ignore")
            } finally {
                inputReader?.close()
                outputWriter?.close()
                inputReader = null
                outputWriter = null
                socket = null
                println("已断开连接")
            }
            // 移除端口转发
            "adb forward --remove tcp:8888".simpleShell()
        }
    }

    /**
     * 发送数据到Android设备
     */
    fun sendData(data: String) {
        println("发送数据: $data")
        try {
            if (socket == null || socket?.isConnected == false || outputWriter == null) {
                println("发送数据失败：未连接")
                return
            }
            outputWriter?.write("${data}\n")
            outputWriter?.flush()
        } catch (e: Exception) {
            println("发送数据失败: ${e.message}")
            disconnect()
        }
    }

    /**
     * 启动接收线程
     */
    private suspend fun startReceiving(socket: Socket, inputReader: BufferedReader) {
        withContext(Dispatchers.IO) {
            try {
                while (socket.isConnected) {
                    val data = inputReader.readLine()
                    // 处理接收到的数据
                    println("[${Thread.currentThread().name}]从Sophon服务端接收到数据: $data")
                }
            } catch (e: Exception) {
                println("接收失败: ${e.message}")
                if (socket.isConnected) {
                    disconnect()
                }
            }
        }
    }
}