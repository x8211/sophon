package sophon.desktop.core

/**
 * 平台相关的 Shell 执行策略（Strategy Pattern）。
 * 不同操作系统提供各自的实现，由 [ShellExecutor.create] 工厂方法自动选择。
 */
interface ShellExecutor {

    fun createProcess(command: String, redirectErrorStream: Boolean): Process

    /** 将命令中的通用 Unix 工具名替换为当前平台的等效命令 */
    fun adaptCommand(command: String): String

    companion object {
        fun create(): ShellExecutor {
            val os = System.getProperty("os.name").lowercase()
            return if (os.contains("windows")) WindowsShellExecutor() else UnixShellExecutor()
        }
    }
}

class UnixShellExecutor : ShellExecutor {

    override fun createProcess(command: String, redirectErrorStream: Boolean): Process =
        ProcessBuilder("/bin/bash", "-c", command)
            .redirectErrorStream(redirectErrorStream)
            .start()

    override fun adaptCommand(command: String): String = command
}

class WindowsShellExecutor : ShellExecutor {

    override fun createProcess(command: String, redirectErrorStream: Boolean): Process =
        ProcessBuilder("cmd", "/c", command)
            .redirectErrorStream(redirectErrorStream)
            .start()

    override fun adaptCommand(command: String): String =
        command.replace("grep", "findstr")
}
