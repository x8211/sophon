package sophon.desktop

import org.junit.Test
import java.io.File

abstract class AbstractParserTest<T> {

    @Test
    fun start() {
        // 1. 读取文件内容
        val resourceUrl = javaClass.classLoader.getResource(fileName())
            ?: throw IllegalArgumentException("测试资源文件未找到")
        val file = File(resourceUrl.toURI())
        val content = file.readText()

        // 2. 解析文件内容
        val result = parse(content)

        // 3. 打印解析结果
        if (onPrintResult(result)) return //如果子类自己处理了打印，就不再打印通用信息
        println("=== 解析器（${javaClass.simpleName}）解析结果 ===")
        println(result)
    }

    abstract fun fileName(): String

    abstract fun parse(content: String): T

    open fun onPrintResult(result: T): Boolean = false
}