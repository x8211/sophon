package sophon.desktop

import sophon.desktop.feature.taskrecord.ActivityTaskParser
import java.io.File
import kotlin.test.Test

/**
 * dumpsys信息解析器测试类
 * 解析Android dumpsys输出，提取Activity栈和Fragment栈信息
 */
class DumpsysParserTest {

    /**
     * Activity信息数据类
     */
    data class ActivityInfo(
        val packageName: String,
        val activityName: String,
        val pid: String,
        val fragments: List<FragmentInfo> = emptyList()
    ) {
        companion object {
            fun parseFromDumpsys(lines: List<String>): ActivityInfo {
                val pattern = """^ACTIVITY\s+([^/]+)/(\S+).*pid=(\d+)""".toRegex()
                val matchResult = pattern.find(lines.first())
                //第一次遍历：收集Fragment粗略信息
                var startCollecting = false
                val fragmentBriefs = mutableListOf<String>()
                for (line in lines) {
                    if (line.startsWith("Added Fragments:")) {
                        startCollecting = true
                        continue
                    }
                    if (startCollecting) {
                        if (line.trim().startsWith("#")) {
                            fragmentBriefs.add(line.substringAfter(": "))
                        } else startCollecting = false
                    }
                }
                //第二次遍历，精准匹配并解析Fragment信息
                val fragmentDetailTexts = mutableListOf<List<String>>()
                var startIndex: Int = -1
                lines.forEachIndexed { index, line ->
                    if (fragmentBriefs.indexOfFirst { it == line } > -1) {
                        startIndex = index
                    }
                    if (line.startsWith("mView=")) {
                        if (startIndex > -1) {
                            fragmentDetailTexts.add(lines.subList(startIndex, index))
                            startIndex = -1
                        }
                    }
                }
                val fragmentList1 = fragmentDetailTexts.map { FragmentInfo.detailParse(it) }
                val fragmentMap = fragmentList1.groupBy { it.parentFragment }
                val fragments = fragmentList1.map {
                    it.copy(childFragments = fragmentMap[it.className] ?: emptyList())
                }

                return ActivityInfo(
                    packageName = matchResult?.groups[1]?.value?.substringBefore(" ") ?: "",
                    activityName = matchResult?.groups[2]?.value?.trim() ?: "",
                    pid = matchResult?.groups[3]?.value ?: "",
                    fragments = fragments.filter { it.parentFragment == null }
                )
            }
        }
    }

    /**
     * Fragment信息数据类（支持嵌套Fragment）
     */
    data class FragmentInfo(
        val index: String = "",
        val plainText: String = "", //dumpsys中的原始文本
        val className: String = "",
        val containerId: String = "",
        val tag: String? = null,
        val state: String = "",
        val who: String = "",
        val parentFragment: String? = null, // 父Fragment的className
        val childFragments: List<FragmentInfo> = emptyList()
    ) {
        override fun toString(): String {
            return "$className(parent:$parentFragment, children:${childFragments})"
        }

        companion object {
            /**
             * 详细解析
             */
            fun detailParse(lines: List<String>): FragmentInfo {
                val str = lines.joinToString { it }
                val pattern =
                    """^(\w+)\{.*?\(.*?(?: tag=(?:(\w+)|null))?.*? mState=(\d+)(?:.*?mParentFragment=([^\s{]+))?""".toRegex()
                val matchResult = pattern.find(str)

                return FragmentInfo(
                    index = "0",
                    plainText = "str",
                    className = matchResult?.groups?.get(1)?.value ?: "",
                    tag = matchResult?.groups?.get(2)?.value,
                    state = matchResult?.groups?.get(3)?.value ?: "",
                    parentFragment = matchResult?.groups?.get(4)?.value
                )
            }
        }
    }

    /**
     * dumpsys解析结果根节点
     */
    data class DumpsysInfo(
        val activities: List<ActivityInfo>
    )

    @Test
    fun testParseDumpsys() {
        // 1. 读取dumpsys.txt文件内容
        val resourcePath =
            "/Users/mico/projects/MyApplication/composeApp/src/desktopTest/resources/dumpsys.txt"
        val file = File(resourcePath)
        if (!file.exists()) {
            throw IllegalArgumentException("dumpsys.txt文件不存在: $resourcePath")
        }

        val result = ActivityTaskParser.parse(file.readText())
        println(result)

    }

}