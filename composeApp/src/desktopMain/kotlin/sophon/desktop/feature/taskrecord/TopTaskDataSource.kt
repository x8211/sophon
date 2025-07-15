package sophon.desktop.feature.taskrecord

import sophon.desktop.core.Shell.oneshotShell
import kotlin.math.max

/**
 * 栈顶Task数据源
 */
class TopTaskDataSource {

    suspend fun queryPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }

    suspend fun queryDetail(packageName: String): TaskRecord {
        packageName.ifBlank { return TaskRecord() }
        return "adb shell dumpsys activity $packageName".oneshotShell(transform = {
            val activityRecords: MutableList<List<LifecycleComponent>> = mutableListOf()
            it.split("ACTIVITY").forEachIndexed { index, s ->
                if (index > 0) {
                    val activityRecord = parseActivityInfo(s)
                    activityRecords.add(activityRecord)
                }
            }
            TaskRecord(
                "",
                packageName,
                activityRecords.reversed().flatten()
            )
        })
    }

    private fun parseActivityInfo(input: String): List<LifecycleComponent> {
        val result = mutableListOf<LifecycleComponent>()
        val fragmentMap = mutableMapOf<Int, MutableList<LifecycleComponent.FragmentInfo>>()

        //解析Activity包名和类名
        val activityInfoResult = activityInfoRegex.find(input) ?: return listOf()
        with(activityInfoResult) {
            val packageName = groupValues[1]
            val className = groupValues[2]
            val activity = LifecycleComponent.ActivityRecord(
                packageName,
                if (className.startsWith(".")) packageName + className else className,
                0,
                groupValues[3].toBoolean(),
                groupValues[4].toBoolean(),
                groupValues[5].toBoolean()
            )
            result.add(activity)
        }

        // 解析Fragment信息
        val fragmentInfoResults = fragmentInfoRegex.findAll(input)
        fragmentInfoResults.forEach {
            val startIndent = it.value.startIndent()
            val fragment = LifecycleComponent.FragmentInfo(
                it.groupValues[1].trim(),
                startIndent,
                it.groupValues[2].toString(),
                it.groupValues[3].toInt(),
                it.groupValues[5].toBoolean()
            )
            
            // 根据缩进级别组织Fragment层级关系
            val level = startIndent / 4 // 假设每级缩进为4个空格
            if (!fragmentMap.containsKey(level)) {
                fragmentMap[level] = mutableListOf()
            }
            fragmentMap[level]?.add(fragment)
        }

        // 构建Fragment层级关系
        val sortedLevels = fragmentMap.keys.sorted()
        for (i in sortedLevels.size - 1 downTo 0) {
            val currentLevel = sortedLevels[i]
            val fragments = fragmentMap[currentLevel] ?: continue
            
            if (i > 0) {
                val parentLevel = sortedLevels[i - 1]
                val parentFragments = fragmentMap[parentLevel] ?: continue
                
                // 将当前层级的Fragment分配给父级Fragment
                fragments.forEach { fragment ->
                    val parentFragment = parentFragments.find { it.level < fragment.level }
                    if (parentFragment != null) {
                        val updatedParent = parentFragment.copy(
                            children = parentFragment.children + fragment
                        )
                        parentFragments[parentFragments.indexOf(parentFragment)] = updatedParent
                    }
                }
            } else {
                // 最顶层的Fragment直接添加到Activity的子组件中
                val activity = result[0] as LifecycleComponent.ActivityRecord
                result[0] = activity.copy(children = fragments)
            }
        }

        return result
    }

    /**
     * 统计字符串前缩进数量
     */
    private fun String.startIndent(): Int {
        var count = 0
        this.toCharArray().forEach {
            if (!it.isWhitespace()) return count
            count++
        }
        return count
    }

    companion object {
        /**
         * 匹配Activity信息，例如：
         * ACTIVITY com.mico/.main.MainActivity e71f29e pid=15856
         *  Local Activity 8997d78 State:
         *      mResumed=true mStopped=false mFinished=false
         *      mIsInMultiWindowMode=false mIsInPictureInPictureMode=false
         */
        private const val activityNameSection = "(.*)/(.*) .* pid=\\d+"
        private const val activityLocalSection = "*Local Activity.*"
        private const val activityStateSection = "*mResumed=(.*) mStopped=(.*) mFinished=(.*)"
        private const val activityMultiWindowSection =
            "*mIsInMultiWindowMode=.* mIsInPictureInPictureMode=.*"
        private val activityInfoRegex =
            Regex("$activityNameSection\\s$activityLocalSection\\s$activityStateSection\\s$activityMultiWindowSection")

        /**
         * 匹配Fragment信息，例如：
         * AMainLiveFragment{2936ffe} (9d914336-8363-413b-9498-7ebe549ed171 id=0x7f0907a7)
         *   mFragmentId=#7f0907a7 mContainerId=#7f0907a7 mTag=null
         *   mState=5 mWho=9d914336-8363-413b-9498-7ebe549ed171 mBackStackNesting=0
         *   mAdded=true mRemoving=false mFromLayout=false mInLayout=false
         *   mHidden=false mDetached=false mMenuVisible=true mHasMenu=false
         *   mRetainInstance=false mUserVisibleHint=true
         */
        private const val fragmentNameSection = "(\\s*[A-Z].*)\\{[^}]*\\} \\([^)]*\\)"
        private const val fragmentIdSection = "*mFragmentId=.* mContainerId=.* mTag=(.*)"
        private const val fragmentStateSection = "*mState=(\\d) mWho=.* mBackStackNesting=.*"
        private const val fragmentAddedSection =
            "*mAdded=(.*) mRemoving=(.*) mFromLayout=.* mInLayout=.*"
        private const val fragmentHiddenSection =
            "*mHidden=(.*) mDetached=(.*) mMenuVisible=.* mHasMenu=.*"
        private const val fragmentRetainSection = "*mRetainInstance=(.*) mUserVisibleHint=(.*)"
        private val fragmentInfoRegex =
            Regex("$fragmentNameSection\\s$fragmentIdSection\\s$fragmentStateSection\\s$fragmentAddedSection\\s$fragmentHiddenSection\\s$fragmentRetainSection")
    }

}

data class TaskRecord(
    val taskId: String = "",
    val packageName: String = "",
    val lifecycleComponents: List<LifecycleComponent> = emptyList(),
)

sealed class LifecycleComponent(
    open val className: String,
    open val level: Int,
) {
    open val stateString: String = ""
    open val isRunning: Boolean = false
    open val children: List<LifecycleComponent> = emptyList()

    data class ActivityRecord(
        val packageName: String,
        override val className: String,
        override val level: Int,
        val resumed: Boolean,
        val stopped: Boolean,
        val finished: Boolean,
        override val children: List<LifecycleComponent> = emptyList(),
    ) : LifecycleComponent(className, level) {

        override val stateString: String
            get() = state.name

        override val isRunning: Boolean
            get() = resumed

        val state = if (resumed) ActivityState.RESUMED
        else if (stopped) ActivityState.STARTED
        else if (finished) ActivityState.DESTROYED
        else ActivityState.INITIALIZED

        enum class ActivityState {
            DESTROYED,
            INITIALIZED,
            CREATED,
            STARTED,
            RESUMED
        }
    }

    data class FragmentInfo(
        override val className: String,
        override val level: Int,
        val tag: String,
        val stateNum: Int,
        val isHidden: Boolean = false,
        override val children: List<LifecycleComponent> = emptyList(),
    ) : LifecycleComponent(className, level) {

        override val stateString: String
            get() = state.name

        override val isRunning: Boolean
            get() = state == FragmentState.RESUMED

        val state = when (stateNum) {
            0 -> FragmentState.ATTACHED
            1 -> FragmentState.CREATED
            2 -> FragmentState.VIEW_CREATED
            3 -> FragmentState.AWAITING_EXIT_EFFECTS
            4 -> FragmentState.ACTIVITY_CREATED
            5 -> FragmentState.STARTED
            6 -> FragmentState.AWAITING_ENTER_EFFECTS
            7 -> FragmentState.RESUMED
            else -> FragmentState.INITIALIZING
        }

        /**
         * 官方定义：Androidx Fragment生命周期状态
         *
         * static final int INITIALIZING = -1;          // Not yet attached.
         * static final int ATTACHED = 0;               // Attached to the host.
         * static final int CREATED = 1;                // Created.
         * static final int VIEW_CREATED = 2;           // View Created.
         * static final int AWAITING_EXIT_EFFECTS = 3;  // Downward state, awaiting exit effects
         * static final int ACTIVITY_CREATED = 4;       // Fully created, not started.
         * static final int STARTED = 5;                // Created and started, not resumed.
         * static final int AWAITING_ENTER_EFFECTS = 6; // Upward state, awaiting enter effects
         * static final int RESUMED = 7;                // Created started and resumed.
         */
        enum class FragmentState {
            INITIALIZING,
            ATTACHED,
            CREATED,
            VIEW_CREATED,
            AWAITING_EXIT_EFFECTS,
            ACTIVITY_CREATED,
            STARTED,
            AWAITING_ENTER_EFFECTS,
            RESUMED,
        }
    }
}

fun TaskRecord.formatLevel(): TaskRecord {
    val result = mutableListOf<LifecycleComponent>()
    var formattedLevel = 0
    var lastLevel = 0
    lifecycleComponents.forEach {
        formattedLevel = max(
            0,
            if (it.level > lastLevel) ++formattedLevel
            else if (it.level < lastLevel) --formattedLevel
            else formattedLevel
        )
        val newValue = when (it) {
            is LifecycleComponent.ActivityRecord -> it.copy(level = formattedLevel)
            is LifecycleComponent.FragmentInfo -> it.copy(level = formattedLevel)
        }
        result.add(newValue)
        lastLevel = it.level
    }
    return copy(lifecycleComponents = result)
}