package sophon.desktop.feature.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell

class DeveloperViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = DeveloperUiState(
                debugLayout = getDebugLayout(),
                hwUi = getHwUi(),
                showTouches = getShowTouches(),
                pointerLocation = getPointerLocation(),
                strictMode = getStrictMode(),
                forceRtl = getForceRtl(),
                stayAwake = getStayAwake(),
                showAllANRs = getShowAllANRs(),
                windowAnimationScale = getWindowAnimationScale(),
                transitionAnimationScale = getTransitionAnimationScale(),
                animatorDurationScale = getAnimatorDurationScale(),
                dontKeepActivities = getDontKeepActivities(),
            )
        }
    }

    /**
     * 切换布局边界开关
     */
    fun toggleDebugLayout() {
        viewModelScope.launch {
            "adb shell setprop debug.layout ${_uiState.value.debugLayout.not()}".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(debugLayout = getDebugLayout()) }
        }
    }

    /**
     * 切换HWUI呈现模式分析
     */
    fun toggleHwUi() {
        viewModelScope.launch {
            "adb shell setprop debug.hwui.profile ${if (_uiState.value.hwUi) "false" else "visual_bars"}".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(hwUi = getHwUi()) }
        }
    }

    fun toggleShowTouches() {
        viewModelScope.launch {
            if (_uiState.value.showTouches) {
                // 关闭显示触摸操作
                "adb shell settings put system show_touches 0".simpleShell()
            } else {
                // 启用显示触摸操作
                "adb shell settings put system show_touches 1".simpleShell()
            }
            refreshSetting()
            _uiState.update { it.copy(showTouches = getShowTouches()) }
        }
    }

    fun togglePointerLocation() {
        viewModelScope.launch {
            if (_uiState.value.pointerLocation) {
                // 关闭指针位置
                "adb shell settings put system pointer_location 0".simpleShell()
            } else {
                // 启用指针位置
                "adb shell settings put system pointer_location 1".simpleShell()
            }
            refreshSetting()
            _uiState.update { it.copy(pointerLocation = getPointerLocation()) }
        }
    }

    fun toggleStrictMode() {
        viewModelScope.launch {
            if (_uiState.value.strictMode) {
                // 关闭严格模式
                "adb shell setprop debug.strict 0".simpleShell()
                "adb shell setprop debug.strict.thread 0".simpleShell()
                "adb shell setprop debug.strict.vm 0".simpleShell()
                "adb shell setprop debug.strict.disk 0".simpleShell()
                "adb shell setprop debug.strict.network 0".simpleShell()
            } else {
                // 启用严格模式
                "adb shell setprop debug.strict 1".simpleShell()
                "adb shell setprop debug.strict.thread 1".simpleShell()
                "adb shell setprop debug.strict.vm 1".simpleShell()
                "adb shell setprop debug.strict.disk 1".simpleShell()
                "adb shell setprop debug.strict.network 1".simpleShell()
                // 启用开发设置
                "adb shell settings put global development_settings_enabled 1".simpleShell()
            }
            refreshSetting()
            _uiState.update { it.copy(strictMode = getStrictMode()) }
        }
    }


    fun toggleForceRtl() {
        viewModelScope.launch {
            if (_uiState.value.forceRtl) {
                // 关闭强制RTL布局
                "adb shell settings put global development_force_rtl 0".simpleShell()
                "adb shell setprop debug.force_rtl 0".simpleShell()
                "adb shell setprop debug.layout.force_rtl 0".simpleShell()
            } else {
                // 启用强制RTL布局
                "adb shell settings put global development_force_rtl 1".simpleShell()
                "adb shell setprop debug.force_rtl 1".simpleShell()
                "adb shell setprop debug.layout.force_rtl 1".simpleShell()
                // 启用开发设置
                "adb shell settings put global development_settings_enabled 1".simpleShell()
            }
            refreshSetting()
            _uiState.update { it.copy(forceRtl = getForceRtl()) }
        }
    }

    fun toggleStayAwake() {
        viewModelScope.launch {
            "adb shell settings put global stay_on_while_plugged_in ${if (_uiState.value.stayAwake) "0" else "3"}".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(stayAwake = getStayAwake()) }
        }
    }

    fun toggleShowAllANRs() {
        viewModelScope.launch {
            if (_uiState.value.showAllANRs) {
                // 关闭显示所有ANR
                "adb shell setprop debug.show_all_anrs 0".simpleShell()
                "adb shell setprop debug.anr.show 0".simpleShell()
            } else {
                // 启用显示所有ANR
                "adb shell setprop debug.show_all_anrs 1".simpleShell()
                "adb shell setprop debug.anr.show 1".simpleShell()
                // 设置ANR对话框显示时间
                "adb shell setprop debug.anr.dialog_timeout 5000".simpleShell()
            }
            refreshSetting()
            _uiState.update { it.copy(showAllANRs = getShowAllANRs()) }
        }
    }

    /**
     * 切换不保留活动开关
     */
    fun toggleDontKeepActivities() {
        viewModelScope.launch {
            if (_uiState.value.dontKeepActivities) {
                // 关闭不保留活动
                "adb shell settings put global always_finish_activities 0".simpleShell()
            } else {
                // 启用不保留活动
                "adb shell settings put global always_finish_activities 1".simpleShell()
                // 启用开发设置
                "adb shell settings put global development_settings_enabled 1".simpleShell()
            }
            refreshSetting()
            _uiState.update { it.copy(dontKeepActivities = getDontKeepActivities()) }
        }
    }

    fun toggleWindowAnimationScale() {
        viewModelScope.launch {
            val newScale = when (_uiState.value.windowAnimationScale) {
                0.0f -> 0.5f
                0.5f -> 1.0f
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 0.0f
            }
            "adb shell settings put global window_animation_scale $newScale".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(windowAnimationScale = getWindowAnimationScale()) }
        }
    }

    fun setWindowAnimationScale(scale: Float) {
        viewModelScope.launch {
            "adb shell settings put global window_animation_scale $scale".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(windowAnimationScale = getWindowAnimationScale()) }
        }
    }

    fun toggleTransitionAnimationScale() {
        viewModelScope.launch {
            val newScale = when (_uiState.value.transitionAnimationScale) {
                0.0f -> 0.5f
                0.5f -> 1.0f
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 0.0f
            }
            "adb shell settings put global transition_animation_scale $newScale".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(transitionAnimationScale = getTransitionAnimationScale()) }
        }
    }

    fun setTransitionAnimationScale(scale: Float) {
        viewModelScope.launch {
            "adb shell settings put global transition_animation_scale $scale".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(transitionAnimationScale = getTransitionAnimationScale()) }
        }
    }

    fun toggleAnimatorDurationScale() {
        viewModelScope.launch {
            val newScale = when (_uiState.value.animatorDurationScale) {
                0.0f -> 0.5f
                0.5f -> 1.0f
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 0.0f
            }
            "adb shell settings put global animator_duration_scale $newScale".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(animatorDurationScale = getAnimatorDurationScale()) }
        }
    }

    fun setAnimatorDurationScale(scale: Float) {
        viewModelScope.launch {
            "adb shell settings put global animator_duration_scale $scale".simpleShell()
            refreshSetting()
            _uiState.update { it.copy(animatorDurationScale = getAnimatorDurationScale()) }
        }
    }

    private suspend fun getDebugLayout() =
        "adb shell getprop debug.layout".oneshotShell { it.contains("true") } == true

    private suspend fun getHwUi() =
        "adb shell getprop debug.hwui.profile".oneshotShell { it.contains("visual_bars") } == true

    private suspend fun getShowTouches() =
        "adb shell settings get system show_touches".oneshotShell { it.contains("1") } == true

    private suspend fun getPointerLocation() =
        "adb shell settings get system pointer_location".oneshotShell { it.contains("1") } == true

    private suspend fun getStrictMode() =
        "adb shell getprop debug.strict".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.strict.thread".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.strict.vm".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.strict.disk".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.strict.network".oneshotShell { it.contains("1") } == true

    private suspend fun getShowCpuUsage() =
        "adb shell getprop debug.cpu_usage_overlay".oneshotShell { it.contains("1") } == true

    private suspend fun getStayAwake() =
        "adb shell settings get global stay_on_while_plugged_in".oneshotShell { it.contains("3") } == true

    private suspend fun getShowAllANRs() =
        "adb shell getprop debug.show_all_anrs".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.anr.show".oneshotShell { it.contains("1") } == true

    private suspend fun getDontKeepActivities() =
        "adb shell settings get global always_finish_activities".oneshotShell { it.contains("1") } == true

    /**
     * You might be wondering where the 1599295570 number came from.
     * This is called SYSPROPS_TRANSACTION and used by settings app to refresh the setting.
     */
    private suspend fun refreshSetting() {
        "adb shell service call activity 1599295570".simpleShell()
    }

    private suspend fun getForceRtl() =
        "adb shell settings get global development_force_rtl".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.force_rtl".oneshotShell { it.contains("1") } == true &&
                "adb shell getprop debug.layout.force_rtl".oneshotShell { it.contains("1") } == true

    private suspend fun getWindowAnimationScale(): Float =
        "adb shell settings get global window_animation_scale".oneshotShell {
            it.trim().toFloatOrNull() ?: 1.0f
        } ?: 1.0f

    private suspend fun getTransitionAnimationScale(): Float =
        "adb shell settings get global transition_animation_scale".oneshotShell {
            it.trim().toFloatOrNull() ?: 1.0f
        } ?: 1.0f

    private suspend fun getAnimatorDurationScale(): Float =
        "adb shell settings get global animator_duration_scale".oneshotShell {
            it.trim().toFloatOrNull() ?: 1.0f
        } ?: 1.0f
}

data class DeveloperUiState(
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