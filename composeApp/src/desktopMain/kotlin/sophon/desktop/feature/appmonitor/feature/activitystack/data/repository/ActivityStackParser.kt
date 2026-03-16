package sophon.desktop.feature.appmonitor.feature.activitystack.data.repository

import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.ActivityInfo
import sophon.desktop.feature.appmonitor.feature.activitystack.domain.model.FragmentInfo

/**
 * 解析 dumpsys activity 输出的工具类
 */
internal object ActivityStackParser {

    fun parse(dumpsys: String): List<ActivityInfo> {
        //第一步：去掉每一行开头的空格
        val lines = dumpsys.lines().map { it.trimStart() }
        //第二步：获取Activity信息块
        val originActivities = mutableListOf<List<String>>()
        var lastIndex = 0
        lines.forEachIndexed { index, string ->
            if (string.startsWith("ACTIVITY") || index == lines.size - 1) {
                originActivities.add(lines.subList(lastIndex, index))
                lastIndex = index
            }
        }
        if (originActivities.isNotEmpty()) {
            originActivities.removeFirst()
        }
        //第三步：去掉无用信息
        return originActivities
            .map {
                val choreIndex = it.indexOfFirst { l -> l.startsWith("Choreographer") }
                val subList1 = if (choreIndex != -1) it.subList(0, choreIndex) else it
                
                val resIndex = it.indexOfFirst { l -> l.startsWith("ResourcesManager") }
                val subList2 = if (resIndex != -1) it.subList(resIndex, it.size) else emptyList()
                
                subList1 + subList2
            }
            .map { lines -> parseActivityFromDumpsys(lines) }
    }

    private fun parseActivityFromDumpsys(lines: List<String>): ActivityInfo {
        val str = lines.take(5).joinToString()
        val pattern = """^ACTIVITY\s+([^/]+)/(\S+).*pid=(\d+)""".toRegex()
        val matchResult = pattern.find(str)

        // 解析mResumed
        val resumePattern = """mResumed=(true|false)""".toRegex()
        val resumed = resumePattern.find(str)?.groups?.get(1)?.value?.toBoolean() ?: false

        // 解析mStopped
        val stopPattern = """mStopped=(true|false)""".toRegex()
        val stopped = stopPattern.find(str)?.groups?.get(1)?.value?.toBoolean() ?: false

        // 解析mFinished
        val finishPattern = """mFinished=(true|false)""".toRegex()
        val finished = finishPattern.find(str)?.groups?.get(1)?.value?.toBoolean() ?: false

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
        
        // 解析 Fragment 详情
        val fragmentList1 = fragmentDetailTexts.map { parseFragmentDetail(it) }
        val fragmentMap = fragmentList1.groupBy { it.parentFragment }
        val fragments = fragmentList1.map {
            it.copy(childFragments = fragmentMap[it.className] ?: emptyList())
        }

        return ActivityInfo(
            packageName = matchResult?.groups?.get(1)?.value?.substringBefore(" ") ?: "",
            activityName = matchResult?.groups?.get(2)?.value?.trim() ?: "",
            pid = matchResult?.groups?.get(3)?.value ?: "",
            resumed = resumed,
            stopped = stopped,
            finished = finished,
            fragments = fragments.filter { it.parentFragment == null }
        )
    }

    private fun parseFragmentDetail(lines: List<String>): FragmentInfo {
        val str = lines.joinToString(" ") { it.trim() }

        // 解析类名
        val classNamePattern = """^(\w+)\{""".toRegex()
        val className = classNamePattern.find(str)?.groups?.get(1)?.value ?: ""

        // 解析mTag
        val tagPattern = """mTag=([^,\s]+)""".toRegex()
        val tagMatch = tagPattern.find(str)
        val tag = when (val tagValue = tagMatch?.groups?.get(1)?.value) {
            "null" -> null
            else -> tagValue
        }

        // 解析mState
        val statePattern = """mState=(\d+)""".toRegex()
        val stateValue = statePattern.find(str)?.groups?.get(1)?.value?.toIntOrNull() ?: -1
        val state = FragmentInfo.State.entries.firstOrNull { it.code == stateValue } ?: FragmentInfo.State.INITIALIZING

        // 解析mHidden
        val hiddenPattern = """mHidden=(true|false)""".toRegex()
        val hidden = hiddenPattern.find(str)?.groups?.get(1)?.value?.toBoolean() ?: false

        // 解析mUserVisibleHint
        val userVisibleHintPattern = """mUserVisibleHint=(true|false)""".toRegex()
        val userVisibleHint =
            userVisibleHintPattern.find(str)?.groups?.get(1)?.value?.toBoolean() ?: true

        // 解析mContainer
        val containerPattern = """mContainer=([^}]+\})""".toRegex()
        val container = containerPattern.find(str)?.groups?.get(1)?.value ?: ""

        // 解析mParentFragment
        val parentFragmentPattern = """mParentFragment=([^\s{]+)""".toRegex()
        val parentFragment = parentFragmentPattern.find(str)?.groups?.get(1)?.value

        // 解析mWho
        val whoPattern = """mWho=([^\s]+)""".toRegex()
        val who = whoPattern.find(str)?.groups?.get(1)?.value ?: ""

        return FragmentInfo(
            index = "0",
            className = className,
            tag = tag,
            state = state,
            who = who,
            parentFragment = parentFragment,
            hidden = hidden,
            userVisibleHint = userVisibleHint,
            container = container
        )
    }
}
