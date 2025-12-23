package sophon.desktop.feature.device.data.repository

import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.device.domain.model.DeviceInfoItem
import sophon.desktop.feature.device.domain.model.DeviceInfoSection
import sophon.desktop.feature.device.domain.repository.DeviceInfoRepository

/**
 * 设备信息仓库的实现类
 * 负责通过 ADB Shell 命令获取设备信息并解析
 */
class DeviceInfoRepositoryImpl : DeviceInfoRepository {

    override suspend fun  getDeviceInfo(): List<DeviceInfoSection> {
        val sections = mutableListOf<DeviceInfoSection>()
        
        sections.add(getBasicInfo())
        sections.add(getSystemInfo())
        sections.add(getScreenInfo())
        sections.add(getCpuInfo())

        return sections
    }

    private suspend fun getBasicInfo(): DeviceInfoSection {
        val items = mutableListOf<DeviceInfoItem>()
        // 获取所有属性
        val propOutput = "adb shell getprop".simpleShell()
        
        val propMap = parseGetProp(propOutput)
        
        // 映射基本信息
        propMap.forEach { (key, value) ->
            when {
                key.contains("ro.product.model") -> items.add(DeviceInfoItem("设备型号", value))
                key.contains("ro.product.brand") -> items.add(DeviceInfoItem("品牌", value))
                key.contains("ro.product.manufacturer") -> items.add(DeviceInfoItem("制造商", value))
                key.contains("ro.product.device") -> items.add(DeviceInfoItem("设备代号", value))
                key.contains("ro.product.name") -> items.add(DeviceInfoItem("产品名称", value))
                key.contains("ro.serialno") -> items.add(DeviceInfoItem("序列号", value))
                key.contains("ro.product.board") -> items.add(DeviceInfoItem("主板", value))
            }
        }
        
        return DeviceInfoSection("基本信息", items)
    }

    private suspend fun getSystemInfo(): DeviceInfoSection {
        val items = mutableListOf<DeviceInfoItem>()
        val propOutput = "adb shell getprop".simpleShell()
        val propMap = parseGetProp(propOutput)

        propMap.forEach { (key, value) ->
            when {
                key.contains("ro.build.version.release") -> items.add(DeviceInfoItem("Android版本", value))
                key.contains("ro.build.version.sdk") -> items.add(DeviceInfoItem("SDK版本", value))
                key.contains("ro.build.id") -> items.add(DeviceInfoItem("构建ID", value))
                key.contains("ro.build.version.incremental") -> items.add(DeviceInfoItem("增量版本", value))
                key.contains("ro.build.version.security_patch") -> items.add(DeviceInfoItem("安全补丁级别", value))
                key.contains("ro.build.fingerprint") -> items.add(DeviceInfoItem("构建指纹", value))
                key.contains("ro.build.display.id") -> items.add(DeviceInfoItem("显示ID", value))
                key.contains("ro.build.type") -> items.add(DeviceInfoItem("构建类型", value))
                key.contains("ro.build.user") -> items.add(DeviceInfoItem("构建用户", value))
                key.contains("ro.build.host") -> items.add(DeviceInfoItem("构建主机", value))
            }
        }
        
        return DeviceInfoSection("系统版本", items)
    }

    private suspend fun getScreenInfo(): DeviceInfoSection {
        val items = mutableListOf<DeviceInfoItem>()

        // 获取屏幕尺寸
        val sizeOutput = "adb shell wm size".simpleShell()
        sizeOutput.lines().forEach { line ->
            if (line.contains("Physical size")) {
                items.add(DeviceInfoItem("物理分辨率", line.substringAfter(":").trim()))
            } else if (line.contains("Override size")) {
                items.add(DeviceInfoItem("覆盖分辨率", line.substringAfter(":").trim()))
            }
        }

        // 获取屏幕密度
        val densityOutput = "adb shell wm density".simpleShell()
        densityOutput.lines().forEach { line ->
            if (line.contains("Physical density")) {
                items.add(DeviceInfoItem("物理密度", line.substringAfter(":").trim()))
            } else if (line.contains("Override density")) {
                items.add(DeviceInfoItem("覆盖密度", line.substringAfter(":").trim()))
            }
        }

        return DeviceInfoSection("屏幕信息", items)
    }

    private suspend fun getCpuInfo(): DeviceInfoSection {
        val items = mutableListOf<DeviceInfoItem>()
        
        // 架构信息
        val propOutput = "adb shell getprop".simpleShell()
        val propMap = parseGetProp(propOutput)
        val abi = propMap.entries.find { it.key == "ro.product.cpu.abi" }
        abi?.let { items.add(DeviceInfoItem("架构", it.value)) }

        // 获取 CPU 核心信息
        val cpuInfoShell = "adb shell cat /proc/cpuinfo".simpleShell()
        val cores = parseCpuCores(cpuInfoShell)
        items.add(DeviceInfoItem("核心数", cores.size.toString()))

        // 尝试获取频率
        try {
            val freqString = "adb shell cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq".simpleShell().trim()
            if (freqString.all { it.isDigit() } && freqString.isNotEmpty()) {
                val freqMhz = freqString.toLong() / 1000
                items.add(DeviceInfoItem("最大频率", "$freqMhz MHz"))
            }
        } catch (e: Exception) {
            // ignore
        }

        return DeviceInfoSection("CPU信息", items)
    }

    // Helper: 解析 getprop 输出 [key]: [value]
    private fun parseGetProp(output: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val lines = output.lines()
        val regex = "\\[([^]]+)]: \\[([^]]*)]".toRegex()

        lines.forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("[") && trimmedLine.contains("]: [")) {
                val matchResult = regex.find(trimmedLine)
                if (matchResult != null) {
                    val key = matchResult.groupValues[1]
                    val value = matchResult.groupValues[2]
                    map[key] = value
                }
            }
        }
        return map
    }

    // Helper: 解析 /proc/cpuinfo
    private fun parseCpuCores(cpuInfoShell: String): List<Map<String, String>> {
        val cpuList = mutableListOf<Map<String, String>>()
        var currentCpuInfo: MutableMap<String, String>? = null

        cpuInfoShell.lines().forEach { line ->
            if (line.isBlank()) return@forEach

            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()

                if (key == "processor") {
                    currentCpuInfo?.let { cpuList.add(it) }
                    currentCpuInfo = mutableMapOf()
                    currentCpuInfo!!["processor"] = value
                } else {
                    currentCpuInfo?.put(key, value)
                }
            }
        }
        currentCpuInfo?.let { cpuList.add(it) }
        return cpuList
    }
}
