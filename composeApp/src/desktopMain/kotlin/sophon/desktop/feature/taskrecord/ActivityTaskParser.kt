package sophon.desktop.feature.taskrecord

object ActivityTaskParser {

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
        originActivities.removeFirst()
        //第三步：去掉无用信息
        return originActivities
            .map {
                val subList1 = it.subList(0, it.indexOfFirst { l -> l.startsWith("Choreographer") })
                val subList2 =
                    it.subList(it.indexOfFirst { l -> l.startsWith("ResourcesManager") }, it.size)
                subList1 + subList2
            }
            .map { lines -> ActivityInfo.parseFromDumpsys(lines) }
    }

}

interface LifecycleComponent {
    fun name(): String
    fun children(): List<LifecycleComponent>
    fun isRunning(): Boolean
    fun stateText(): String
}

/**
 * Activity信息数据类
 */
data class ActivityInfo(
    val packageName: String,
    val activityName: String,
    val pid: String,
    val resumed: Boolean = false,
    val stopped: Boolean = false,
    val finished: Boolean = false,
    val fragments: List<FragmentInfo> = emptyList()
) : LifecycleComponent {
    override fun name(): String = activityName
    override fun children(): List<LifecycleComponent> = fragments
    override fun isRunning(): Boolean = resumed
    override fun stateText(): String =
        if (resumed) "RESUMED" else if (stopped) "STOPPED" else "FINISHED"

    companion object {
        fun parseFromDumpsys(lines: List<String>): ActivityInfo {
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
            val fragmentList1 = fragmentDetailTexts.map { FragmentInfo.detailParse(it) }
            val fragmentMap = fragmentList1.groupBy { it.parentFragment }
            val fragments = fragmentList1.map {
                it.copy(childFragments = fragmentMap[it.className] ?: emptyList())
            }

            return ActivityInfo(
                packageName = matchResult?.groups[1]?.value?.substringBefore(" ") ?: "",
                activityName = matchResult?.groups[2]?.value?.trim() ?: "",
                pid = matchResult?.groups[3]?.value ?: "",
                resumed = resumed,
                stopped = stopped,
                finished = finished,
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
    val className: String = "",
    val tag: String? = null,
    val state: State = State.INITIALIZING,
    val who: String = "",
    val parentFragment: String? = null, // 父Fragment的className
    val childFragments: List<FragmentInfo> = emptyList(),
    val hidden: Boolean = false, // 从mHidden解析
    val userVisibleHint: Boolean = true, // 从mUserVisibleHint解析
    val container: String = "" // 从mContainer解析
) : LifecycleComponent {
    override fun name(): String = className
    override fun children(): List<LifecycleComponent> = childFragments
    override fun isRunning(): Boolean = state == State.RESUMED
    override fun stateText(): String = state.name

    companion object {
        /**
         * 详细解析
         */
        fun detailParse(lines: List<String>): FragmentInfo {
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
            val state = State.entries.firstOrNull { it.code == stateValue } ?: State.INITIALIZING

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

        /**
         * 官方定义：Androidx Fragment生命周期状态
         */
        enum class State(val code: Int) {
            INITIALIZING(-1), // Not yet attached.
            ATTACHED(0), // Attached to the host.
            CREATED(1), // Created.
            VIEW_CREATED(2), // View Created.
            AWAITING_EXIT_EFFECTS(3),// Downward state, awaiting exit effects
            ACTIVITY_CREATED(4), // Fully created, not started.
            STARTED(5),// Created and started, not resumed.
            AWAITING_ENTER_EFFECTS(6),// Upward state, awaiting enter effects
            RESUMED(7) // Created started and resumed.
        }
    }


}