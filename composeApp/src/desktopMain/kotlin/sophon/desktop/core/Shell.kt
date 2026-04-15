package sophon.desktop.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList

object Shell {

    private val executor: ShellExecutor = ShellExecutor.create()

    /**
     * 若命令以 "adb" 开头，自动注入已选设备、平台适配和 adb 路径前缀。
     */
    fun formatIfAdbCmd(input: String): String {
        if (!input.startsWith("adb")) return input
        val state = Context.stream.value
        var command = input
        if (state.selectedDevice.isNotBlank()) {
            command = command.replace("adb", "adb -s ${state.selectedDevice}")
        }
        command = executor.adaptCommand(command)
        return "${state.adbToolPath}${command.removePrefix("adb")}".also {
            println("finalCmd: $it")
        }
    }

    /**
     * 执行Shell命令，流式返回输出
     */
    fun String.streamShell() = flow {
        val cmd = formatIfAdbCmd(this@streamShell)
        val p = executor.createProcess(cmd, redirectErrorStream = true)
        p.inputStream.bufferedReader().use { emit(it.readText()) }
    }.flowOn(Dispatchers.IO)

    /**
     * 执行Shell命令，以二进制流(Flow)方式返回输出
     */
    fun String.byteStreamShell() = flow {
        val cmd = formatIfAdbCmd(this@byteStreamShell)
        val p = executor.createProcess(cmd, redirectErrorStream = false)

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
