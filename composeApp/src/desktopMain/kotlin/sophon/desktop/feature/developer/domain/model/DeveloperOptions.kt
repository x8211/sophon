package sophon.desktop.feature.developer.domain.model

data class DeveloperOptions(
    val debugLayout: Boolean = false, //布局边界
    val hwUi: Boolean = false, //GPU呈现模式
    val showTouches: Boolean = false, //显示触摸操作
    val pointerLocation: Boolean = false, //指针位置
    val strictMode: Boolean = false, //严格模式
    val forceRtl: Boolean = false, //强制RTL布局
    val stayAwake: Boolean = false, //保持唤醒状态
    val showAllANRs: Boolean = false, //显示所有ANR
    val windowAnimationScale: Float = 1.0f, //窗口动画缩放
    val transitionAnimationScale: Float = 1.0f, //过渡动画缩放
    val animatorDurationScale: Float = 1.0f, //Animator时长缩放
    val dontKeepActivities: Boolean = false, //不保留活动
)
