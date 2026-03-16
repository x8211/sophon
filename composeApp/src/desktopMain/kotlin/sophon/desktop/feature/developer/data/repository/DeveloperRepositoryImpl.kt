package sophon.desktop.feature.developer.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.developer.domain.model.DeveloperOptions
import sophon.desktop.feature.developer.domain.repository.DeveloperRepository

class DeveloperRepositoryImpl : DeveloperRepository {

    override suspend fun getOptions(): DeveloperOptions {
        return DeveloperOptions(
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

    override suspend fun setDebugLayout(enabled: Boolean) {
        "adb shell setprop debug.layout $enabled".simpleShell()
        refreshSetting()
    }

    override suspend fun setHwUi(enabled: Boolean) {
        "adb shell setprop debug.hwui.profile ${if (enabled) "visual_bars" else "false"}".simpleShell()
        refreshSetting()
    }

    override suspend fun setShowTouches(enabled: Boolean) {
        "adb shell settings put system show_touches ${if (enabled) 1 else 0}".simpleShell()
        refreshSetting()
    }

    override suspend fun setPointerLocation(enabled: Boolean) {
        "adb shell settings put system pointer_location ${if (enabled) 1 else 0}".simpleShell()
        refreshSetting()
    }

    override suspend fun setStrictMode(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        "adb shell setprop debug.strict $value".simpleShell()
        "adb shell setprop debug.strict.thread $value".simpleShell()
        "adb shell setprop debug.strict.vm $value".simpleShell()
        "adb shell setprop debug.strict.disk $value".simpleShell()
        "adb shell setprop debug.strict.network $value".simpleShell()
        if (enabled) {
            "adb shell settings put global development_settings_enabled 1".simpleShell()
        }
        refreshSetting()
    }

    override suspend fun setForceRtl(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        "adb shell settings put global development_force_rtl $value".simpleShell()
        "adb shell setprop debug.force_rtl $value".simpleShell()
        "adb shell setprop debug.layout.force_rtl $value".simpleShell()
        if (enabled) {
            "adb shell settings put global development_settings_enabled 1".simpleShell()
        }
        refreshSetting()
    }

    override suspend fun setStayAwake(enabled: Boolean) {
        // 3 for stay on while plugged in
        val value = if (enabled) 3 else 0
        "adb shell settings put global stay_on_while_plugged_in $value".simpleShell()
        refreshSetting()
    }

    override suspend fun setShowAllANRs(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        "adb shell setprop debug.show_all_anrs $value".simpleShell()
        "adb shell setprop debug.anr.show $value".simpleShell()
        if (enabled) {
            "adb shell setprop debug.anr.dialog_timeout 5000".simpleShell()
        }
        refreshSetting()
    }

    override suspend fun setDontKeepActivities(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        "adb shell settings put global always_finish_activities $value".simpleShell()
        if (enabled) {
            "adb shell settings put global development_settings_enabled 1".simpleShell()
        }
        refreshSetting()
    }

    override suspend fun setWindowAnimationScale(scale: Float) {
        "adb shell settings put global window_animation_scale $scale".simpleShell()
        refreshSetting()
    }

    override suspend fun setTransitionAnimationScale(scale: Float) {
        "adb shell settings put global transition_animation_scale $scale".simpleShell()
        refreshSetting()
    }

    override suspend fun setAnimatorDurationScale(scale: Float) {
        "adb shell settings put global animator_duration_scale $scale".simpleShell()
        refreshSetting()
    }

    // --- Helpers ---

    private suspend fun refreshSetting() {
        "adb shell service call activity 1599295570".simpleShell()
    }

    private suspend fun getDebugLayout() =
        "adb shell getprop debug.layout".oneshotShell { it.contains("true") }

    private suspend fun getHwUi() =
        "adb shell getprop debug.hwui.profile".oneshotShell { it.contains("visual_bars") }

    private suspend fun getShowTouches() =
        "adb shell settings get system show_touches".oneshotShell { it.contains("1") }

    private suspend fun getPointerLocation() =
        "adb shell settings get system pointer_location".oneshotShell { it.contains("1") }

    private suspend fun getStrictMode() =
        "adb shell getprop debug.strict".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.strict.thread".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.strict.vm".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.strict.disk".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.strict.network".oneshotShell { it.contains("1") }

    private suspend fun getForceRtl() =
        "adb shell settings get global development_force_rtl".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.force_rtl".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.layout.force_rtl".oneshotShell { it.contains("1") }

    private suspend fun getStayAwake() =
        "adb shell settings get global stay_on_while_plugged_in".oneshotShell { it.contains("3") }

    private suspend fun getShowAllANRs() =
        "adb shell getprop debug.show_all_anrs".oneshotShell { it.contains("1") }
                && "adb shell getprop debug.anr.show".oneshotShell { it.contains("1") }

    private suspend fun getDontKeepActivities() =
        "adb shell settings get global always_finish_activities".oneshotShell { it.contains("1") }

    private suspend fun getWindowAnimationScale(): Float =
        "adb shell settings get global window_animation_scale".oneshotShell {
            it.trim().toFloatOrNull() ?: 1.0f
        }

    private suspend fun getTransitionAnimationScale(): Float =
        "adb shell settings get global transition_animation_scale".oneshotShell {
            it.trim().toFloatOrNull() ?: 1.0f
        }

    private suspend fun getAnimatorDurationScale(): Float =
        "adb shell settings get global animator_duration_scale".oneshotShell {
            it.trim().toFloatOrNull() ?: 1.0f
        }
}
