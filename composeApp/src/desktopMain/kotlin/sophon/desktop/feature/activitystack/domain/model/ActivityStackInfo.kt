package sophon.desktop.feature.activitystack.domain.model

/**
 * 生命周期组件接口
 * 代表 Activity 或 Fragment 等具有生命周期的组件
 */
interface LifecycleComponent {
    /** 组件名称 */
    fun name(): String
    
    /** 子组件列表 */
    fun children(): List<LifecycleComponent>
    
    /** 是否正在运行 (Resumed) */
    fun isRunning(): Boolean
    
    /** 状态显示文本 */
    fun stateText(): String
}

/**
 * Activity 信息数据类
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
}

/**
 * Fragment 信息数据类（支持嵌套 Fragment）
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

    /**
     * Androidx Fragment 生命周期状态
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
