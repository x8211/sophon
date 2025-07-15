package sophon.desktop.feature.screen

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.core.Shell.simpleShell

/**
 * 屏幕信息数据源
 */
class ScreenInfoDataSource {

    private val resolutionParser: (String) -> ScreenMetaData = { result ->
        val physicalMatch = parse(result, "Physical .*:(.*)")
        val overrideMatch = parse(result, "Override.*:(.*)")
        ScreenMetaData(
            physicalWidth = physicalMatch.split("x").getOrNull(0) ?: "",
            physicalHeight = physicalMatch.split("x").getOrNull(1) ?: "",
            overrideWidth = overrideMatch.split("x").getOrNull(0) ?: "",
            overrideHeight = overrideMatch.split("x").getOrNull(1) ?: "",
        )
    }

    private val densityParser: (String) -> ScreenMetaData = { result ->
        val physicalMatch = parse(result, "Physical .*:(.*)")
        val overrideMatch = parse(result, "Override.*:(.*)")
        ScreenMetaData(physicalDensity = physicalMatch, overrideDensity = overrideMatch)
    }

    /**
     * 查询屏幕分辨率和屏幕密度
     */
    suspend fun queryScreenInfo(): ScreenMetaData {
        val resolution = RESOLUTION_CMD.oneshotShell(transform = resolutionParser)
        val density = DENSITY_CMD.oneshotShell(transform = densityParser)
        return ScreenMetaData(
            physicalWidth = resolution.physicalWidth,
            physicalHeight = resolution.physicalHeight,
            overrideWidth = resolution.overrideWidth,
            overrideHeight = resolution.overrideHeight,
            physicalDensity = density.physicalDensity,
            overrideDensity = density.overrideDensity
        )
    }

    suspend fun modifyResolution(width: String, height: String) {
        "$RESOLUTION_CMD ${width}x${height}".simpleShell()
    }

    suspend fun modifyDensity(density: String) {
        "$DENSITY_CMD $density".oneshotShell(transform = densityParser)
    }

    suspend fun resetResolution() {
        "$RESOLUTION_CMD reset".oneshotShell { it }
    }

    suspend fun resetDensity() {
        "$DENSITY_CMD reset".oneshotShell(transform = densityParser)
    }

    private fun parse(text: String, regex: String): String =
        regex.toRegex().find(text)?.value?.substringAfter(":")?.trim() ?: ""

    companion object {
        private const val RESOLUTION_CMD = "adb shell wm size"
        private const val DENSITY_CMD = "adb shell wm density"
    }
}

/**
 * 屏幕元数据
 * [physicalWidth] 物理屏幕宽度
 * [physicalHeight] 物理屏幕高度
 * [overrideWidth] 当前屏幕宽度（手动修改的值）
 * [overrideHeight] 当前屏幕高度（手动修改的值）
 * [physicalDensity] 物理屏幕密度
 * [overrideDensity] 当前屏幕密度（手动修改的值）
 * [inputWidth] 输入屏幕宽度
 * [inputHeight] 输入屏幕高度
 * [inputDensity] 输入屏幕密度
 */
data class ScreenMetaData(
    val physicalWidth: String = "unknown",
    val physicalHeight: String = "unknown",
    val physicalDensity: String = "unknown",
    val overrideWidth: String = "",
    val overrideHeight: String = "",
    val overrideDensity: String = "",
    val inputWidth: String = "",
    val inputHeight: String = "",
    val inputDensity: String = "",
)
