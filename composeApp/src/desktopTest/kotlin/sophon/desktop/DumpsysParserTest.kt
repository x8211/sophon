package sophon.desktop

import sophon.desktop.feature.activitystack.ActivityInfo
import sophon.desktop.feature.activitystack.ActivityStackParser
import sophon.desktop.feature.activitystack.LifecycleComponent

/**
 * dumpsys信息解析器测试类
 * 解析Android dumpsys输出，提取Activity栈和Fragment栈信息
 */
class DumpsysParserTest : AbstractParserTest<List<ActivityInfo>>() {

    override fun fileName(): String = "dumpsys.txt"

    override fun parse(content: String) = ActivityStackParser.parse(content)

    override fun onPrintResult(result: List<ActivityInfo>): Boolean {
        result.forEach { printChild(it, 0) }
        return true
    }

    private fun printChild(lifecycleComponent: LifecycleComponent, level: Int) {
        println("${"    ".repeat(level)}${lifecycleComponent.name()}")
        lifecycleComponent.children().forEach {
            printChild(it, level + 1)
        }
    }

}