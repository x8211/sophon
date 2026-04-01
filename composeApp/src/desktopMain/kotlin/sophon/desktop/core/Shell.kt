package sophon.desktop.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList

object Shell {

    /**
     * 执行Shell命令，流式返回输出
     */
    fun String.streamShell() = flow {
        val cmd = Context.formatIfAdbCmd(this@streamShell)
        val p = ProcessBuilder("/bin/bash", "-c", cmd)
            .redirectErrorStream(true)
            .start()
        p.inputStream.bufferedReader().use { emit(it.readText()) }
    }.flowOn(Dispatchers.IO)

    /**
     * 执行Shell命令，以二进制流(Flow)方式返回输出
     */
    fun String.byteStreamShell() = flow {
        val cmd = Context.formatIfAdbCmd(this@byteStreamShell)
        val p = ProcessBuilder("/bin/bash", "-c", cmd)
            .redirectErrorStream(false)
            .start()

        p.inputStream.use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                emit(buffer.copyOfRange(0, bytesRead))
            }
        }

        val exitCode = p.waitFor()
        if (exitCode != 0) {
            val errOutput = p.errorStream.bufferedReader().readText()
            println("✗ shell 命令错误输出: $errOutput")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 执行Shell命令，一次性返回全部输出，使用[transform]解析出需要的内容
     */
    suspend fun <T> String.oneshotShell(transform: (String) -> T): T =
        transform(buildString { streamShell().toList().forEach { append(it) } })

    /**
     * 执行Shell命令，一次性返回全部输出
     */
    suspend fun String.simpleShell() = oneshotShell { it }

}