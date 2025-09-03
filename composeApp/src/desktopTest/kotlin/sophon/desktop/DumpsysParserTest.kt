package sophon.desktop

import sophon.desktop.feature.taskrecord.ActivityTaskParser
import sophon.desktop.feature.taskrecord.LifecycleComponent
import java.io.File
import kotlin.test.Test

/**
 * dumpsys信息解析器测试类
 * 解析Android dumpsys输出，提取Activity栈和Fragment栈信息
 */
class DumpsysParserTest {

    @Test
    fun testParseDumpsys() {
        // 1. 读取dumpsys.txt文件内容
        val resourceUrl = javaClass.classLoader.getResource("dumpsys.txt")
            ?: throw IllegalArgumentException("测试资源文件未找到")
        val file = File(resourceUrl.toURI())

        val result = ActivityTaskParser.parse(file.readText())
        result.forEach { printChild(it, 0) }

    }

    private fun printChild(lifecycleComponent: LifecycleComponent, level: Int) {
        println("${"    ".repeat(level)}${lifecycleComponent.name()}")
        lifecycleComponent.children().forEach {
            printChild(it, level + 1)
        }
    }

}