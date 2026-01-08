package sophon.desktop.feature.gfxmonitor.data.repository

import sophon.desktop.core.Shell.oneshotShell
import sophon.desktop.feature.gfxmonitor.domain.model.DisplayData
import sophon.desktop.feature.gfxmonitor.domain.model.GfxMetrics
import sophon.desktop.feature.gfxmonitor.domain.model.JankReason
import sophon.desktop.feature.gfxmonitor.domain.model.ViewRootInfo
import sophon.desktop.feature.gfxmonitor.domain.repository.GfxMonitorRepository

class GfxMonitorRepositoryImpl : GfxMonitorRepository {
    override suspend fun getDisplayData(): DisplayData {
        return try {
            val packageName = getForegroundPackageName()
            if (packageName.isEmpty()) return DisplayData()

            val output = "adb shell dumpsys gfxinfo $packageName framestats".oneshotShell { it }

            // 1. 解析全局统计指标 (通常在输出的最前面)
            val globalMetrics = parseGfxMetrics(output)

            // 2. 解析 View Hierarchy 及各窗口详情
            val viewHierarchyIndex = output.indexOf("View hierarchy:")
            val viewRootDetails = mutableListOf<ViewRootInfo>()
            
            // 首先通过 "Profile data in ms:" 区域获取各窗口的性能数据快照
            val profileSectionIndex = output.indexOf("Profile data in ms:")
            val profileSection = if (profileSectionIndex != -1) {
                val endIdx = if (viewHierarchyIndex != -1) viewHierarchyIndex else output.length
                output.substring(profileSectionIndex, endIdx)
            } else ""

            // 将 Profile 区域按具体的 ViewRootImpl 拆分
            val windowStatsMap = mutableMapOf<String, GfxMetrics>()
            if (profileSection.isNotEmpty()) {
                val windowChunks = profileSection.split(Regex("\\n\\s+[^\\n]+ViewRootImpl@[a-f0-9]+"))
                val windowNames = Regex("\\s+([^\\n]+ViewRootImpl@[a-f0-9]+)").findAll(profileSection).map { it.groupValues[1].trim() }.toList()
                
                windowNames.forEachIndexed { index, name ->
                    val chunk = windowChunks.getOrNull(index + 1) ?: ""
                    if (chunk.isNotEmpty()) {
                        windowStatsMap[name] = parseGfxMetrics(chunk)
                    }
                }
            }

            // 解析 View Hierarchy 结构并结合上面解析出的性能指标
            if (viewHierarchyIndex != -1) {
                val vhSection = output.substring(viewHierarchyIndex)
                val lines = vhSection.lines().filter { it.isNotBlank() }.drop(1)
                
                var i = 0
                while (i < lines.size && !lines[i].contains("Total ViewRootImpl")) {
                    val line1 = lines[i].trim()
                    if (line1.contains("ViewRootImpl@")) {
                        val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""
                        val statsMatch = Regex("(\\d+)\\s+views,\\s+([\\d\\.]+\\s+\\w+)\\s+of render nodes").find(nextLine)
                        if (statsMatch != null) {
                            // 查找此 ViewRoot 对应的性能指标 (通过名称模糊匹配)
                            val matchedMetrics = windowStatsMap.entries.find { line1.contains(it.key.substringBefore(" (")) }?.value 
                                ?: GfxMetrics()
                                
                            viewRootDetails.add(
                                ViewRootInfo(
                                    name = line1,
                                    views = statsMatch.groupValues[1].toIntOrNull() ?: 0,
                                    renderNodeMemory = statsMatch.groupValues[2],
                                    metrics = matchedMetrics
                                )
                            )
                            i += 2
                            continue
                        }
                    }
                    i++
                }
            }

            val viewHierarchyText = if (viewHierarchyIndex != -1) output.substring(viewHierarchyIndex) else ""
            val totalViewRootImpl = Regex("Total ViewRootImpl\\s+: (\\d+)").find(viewHierarchyText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val totalViews = Regex("Total attached Views : (\\d+)").find(viewHierarchyText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val renderNodeMatch = Regex("Total RenderNode\\s+: ([\\d\\.]+\\s+\\w+) \\(used\\) / ([\\d\\.]+\\s+\\w+) \\(capacity\\)").find(viewHierarchyText)
            
            DisplayData(
                packageName = packageName,
                globalMetrics = globalMetrics,
                totalViewRootImpl = totalViewRootImpl,
                totalViews = totalViews,
                renderNodeUsedMemory = renderNodeMatch?.groupValues?.get(1) ?: "",
                renderNodeCapacityMemory = renderNodeMatch?.groupValues?.get(2) ?: "",
                viewRootDetails = viewRootDetails
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DisplayData()
        }
    }

    /**
     * 解析通用的图形性能指标
     */
    private fun parseGfxMetrics(text: String): GfxMetrics {
        val totalFrames = Regex("Total frames rendered: (\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val jankyMatch = Regex("Janky frames: (\\d+) \\((\\d+\\.\\d+)%\\)").find(text)
        val jankyFrames = jankyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val jankPercentage = jankyMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f

        // CPU
        val p50Cpu = Regex("50th percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val p90Cpu = Regex("90th percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val p95Cpu = Regex("95th percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val p99Cpu = Regex("99th percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

        // GPU
        val p50Gpu = Regex("50th gpu percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val p90Gpu = Regex("90th gpu percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val p95Gpu = Regex("95th gpu percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val p99Gpu = Regex("99th gpu percentile: (\\d+)ms").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

        // Reasons
        val reasons = listOf(
            "Number Missed Vsync" to "错过垂直同步",
            "Number High input latency" to "输入延迟过高",
            "Number Slow UI thread" to "UI线程慢",
            "Number Slow bitmap uploads" to "Bitmap上传慢",
            "Number Slow issue draw commands" to "绘制命令慢",
            "Number Frame deadline missed" to "截止时间错过",
            "Number Frame deadline missed (legacy)" to "截止时间错过(旧)"
        )
        val jankReasonList = reasons.mapNotNull { (key, desc) ->
            val count = Regex("$key: (\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            if (count > 0) JankReason(key, count, desc) else null
        }

        return GfxMetrics(
            totalFrames = totalFrames,
            jankyFrames = jankyFrames,
            jankPercentage = jankPercentage,
            p50Cpu = p50Cpu,
            p90Cpu = p90Cpu,
            p95Cpu = p95Cpu,
            p99Cpu = p99Cpu,
            p50Gpu = p50Gpu,
            p90Gpu = p90Gpu,
            p95Gpu = p95Gpu,
            p99Gpu = p99Gpu,
            jankReasons = jankReasonList
        )
    }

    /**
     * 获取当前前台应用包名
     */
    private suspend fun getForegroundPackageName(): String {
        return "adb shell dumpsys activity activities | grep '* Task{' | head -n 1"
            .oneshotShell { "A=\\d+:(\\S+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: "" }
    }
}