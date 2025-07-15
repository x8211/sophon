package sophon.desktop.feature.device

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.oneshotShell
import kotlinx.coroutines.launch

class DeviceInfoViewModel : StateScreenModel<DeviceInfo>(DeviceInfo()) {

    private val regex = Regex("\\[(.*)]:\\s*\\[(.*)]")

    init {
        screenModelScope.launch {
            Context.stream.collect {
                "adb shell getprop".oneshotShell { str ->
                    val map = mutableMapOf<String, String>()
                    str.lines()
                        .forEach { regex.find(it)?.groupValues?.apply { map[this[1]] = this[2] } }

                    // 按类别组织系统属性
                    val systemInfo = mutableListOf<Pair<String, List<Pair<String, String>>>>()

                    // 基本信息
                    systemInfo.add(
                        "基本信息" to listOf(
                            "设备品牌" to map.safeGet("ro.product.brand"),
                            "设备制造商" to map.safeGet("ro.product.manufacturer"),
                            "内部代号" to map.safeGet("ro.product.model"),
                            "设备名称" to map.safeGet("ro.product.name"),
                            "设备代号" to map.safeGet("ro.product.device"),
                            "Android版本" to map.safeGet("ro.build.version.release"),
                            "SDK版本" to map.safeGet("ro.build.version.sdk"),
                            "安全补丁级别" to map.safeGet("ro.build.version.security_patch"),
                            "构建日期" to map.safeGet("ro.build.date"),
                        )
                    )

                    // 硬件信息
                    systemInfo.add(
                        "硬件信息" to listOf(
                            "CPU型号" to map.safeGet("ro.hardware"),
                            "CPU核心数" to map.safeGet("ro.product.cpu.activecore"),
                            "CPU架构" to map.safeGet("ro.arch"),
                            "GPU型号" to map.safeGet("ro.hardware.gpu"),
                            "屏幕密度" to map.safeGet("ro.sf.lcd_density"),
                        )
                    )


                    // 所有属性（按字母顺序排序）
                    val allProperties = map.entries
                        .map { it.key to it.value }
                        .sortedBy { it.first }

                    mutableState.value = DeviceInfo(
                        sections = systemInfo,
                        allProperties = allProperties
                    )
                }

                "adb shell cat /proc/cpuinfo".oneshotShell { str ->
                    val cpuInfo = parseCpuInfo(str)

                    // 更新状态并保留原有数据
                    mutableState.value = mutableState.value.copy(
                        cpuInfo = cpuInfo,
                        sections = mutableState.value.sections + listOf(
                            "CPU详细信息" to cpuInfo.map { (key, value) ->
                                key to value
                            }
                        )
                    )
                }
            }
        }
    }

    /**
     * 解析/proc/cpuinfo文件内容，提取CPU信息
     */
    private fun parseCpuInfo(cpuInfoStr: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val cpuEntries = cpuInfoStr.split("\n\n").filter { it.startsWith("processor", true) }

        // 如果没有CPU条目，返回空列表
        if (cpuEntries.isEmpty()) {
            return result
        }

        // 获取第一个CPU的详细信息
        val firstCpu = cpuEntries.first()

        // 解析CPU信息
        val cpuMap = firstCpu.lines().associate {
            val split = it.split(":", limit = 2)
            split[0].trim() to split[1].trim()
        }

        // 向结果列表添加重要的CPU信息
        result.add("处理器" to (cpuMap["Processor"] ?: "未知"))
        result.add("硬件平台" to (cpuMap["Hardware"] ?: "未知"))
        result.add("CPU架构" to (cpuMap["CPU architecture"] ?: "未知"))
        result.add("CPU部件" to (cpuMap["CPU part"] ?: "未知"))
        result.add("CPU变体" to (cpuMap["CPU variant"] ?: "未知"))
        result.add("CPU修订版本" to (cpuMap["CPU revision"] ?: "未知"))
        result.add("BogoMIPS" to (cpuMap["BogoMIPS"] ?: "未知"))
        result.add("CPU频率" to (cpuMap["CPU frequency"] ?: "未知"))
        result.add("CPU内核数" to cpuEntries.size.toString())
        result.add("CPU制造商" to (parseCpuImplementor(cpuMap["CPU implementer"]) ?: "未知"))

        // 添加任何其他可能的CPU信息
        cpuMap.forEach { (key, value) ->
            if (key !in listOf(
                    "Processor", "Hardware", "CPU architecture", "CPU part",
                    "CPU variant", "CPU revision", "BogoMIPS", "CPU frequency", "CPU implementer"
                )
            ) {
                if (key != "processor") {
                    result.add(key to value)
                }

            }
        }

        return result
    }

    private fun parseCpuImplementor(str: String?) = when (str) {
        "0x41" -> "ARM"
        "0x4E" -> "英伟达"
        "0x51" -> "高通"
        "0x53" -> "三星"
        "0x61" -> "苹果"
        "0x68" -> "华为（海思半导体）"
        else -> null
    }
}

data class DeviceInfo(
    val sections: List<Pair<String, List<Pair<String, String>>>> = emptyList(),
    val allProperties: List<Pair<String, String>> = emptyList(),
    val cpuInfo: List<Pair<String, String>> = emptyList(),
)

private fun Map<String, String>.safeGet(key: String) = getOrDefault(key, "unknown")