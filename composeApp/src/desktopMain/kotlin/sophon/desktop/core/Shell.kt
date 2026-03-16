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
     * 执行Shell命令，一次性返回全部输出，使用[transform]解析出需要的内容
     */
    suspend fun <T> String.oneshotShell(transform: (String) -> T): T =
        transform(buildString { streamShell().toList().forEach { append(it) } })

    /**
     * 执行Shell命令，一次性返回全部输出
     */
    suspend fun String.simpleShell() = oneshotShell { it }

}