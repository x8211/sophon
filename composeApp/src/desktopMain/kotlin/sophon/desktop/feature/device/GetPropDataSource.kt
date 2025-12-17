package sophon.desktop.feature.device

import sophon.desktop.core.Shell.simpleShell

suspend fun collectInfo(): List<DeviceInfoSection> {
    val basicInfo = DeviceInfoSection.BasicInfo()
    val systemInfo = DeviceInfoSection.SystemInfo()
    val screenInfo = DeviceInfoSection.ScreenInfo()
    val cpuInfo = DeviceInfoSection.CpuInfo()

    // 获取基本信息
    val basicInfoShell = "adb shell getprop".simpleShell()
    basicInfoShell.lines().forEach { line ->
        val trimmedLine = line.trim()
        if (trimmedLine.startsWith("[") && trimmedLine.contains("]: [")) {
            val regex = "\\[([^]]+)]: \\[([^]]*)]".toRegex()
            val matchResult = regex.find(trimmedLine) ?: return@forEach

            val key = matchResult.groupValues[1]
            val value = matchResult.groupValues[2]

            basicInfo.handle(key, value)
            systemInfo.handle(key, value)
            cpuInfo.handle(key, value)
        }
    }

    // 获取屏幕信息
    val sizeOutput = "adb shell wm size".simpleShell()
    sizeOutput.lines().forEach { line ->
        if (line.contains("Physical size")) {
            screenInfo.addItem("物理分辨率", line.substringAfter(":").trim())
        } else if (line.contains("Override size")) {
            screenInfo.addItem("覆盖分辨率", line.substringAfter(":").trim())
        }
    }
    val densityOutput = "adb shell wm density".simpleShell()
    densityOutput.lines().forEach { line ->
        if (line.contains("Physical density")) {
            screenInfo.addItem("物理密度", line.substringAfter(":").trim())
        } else if (line.contains("Override density")) {
            screenInfo.addItem("覆盖密度", line.substringAfter(":").trim())
        }
    }

    //获取CPU信息
    val cpuInfoShell = "adb shell cat /proc/cpuinfo".simpleShell()
    val cores = DeviceInfoSection.CpuInfo.parseCores(cpuInfoShell)
    cpuInfo.addCores(cores)
    cpuInfo.addItem("核心数", cpuInfo.cores.size.toString())

    // 尝试获取频率 (可能需要特定权限，失败忽略)
    try {
        val freqString =
            "adb shell cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq".simpleShell()
                .trim()
        if (freqString.all { it.isDigit() } && freqString.isNotEmpty()) {
            val freqMhz = freqString.toLong() / 1000
            cpuInfo.addItem("最大频率", "$freqMhz MHz")
        }
    } catch (e: Exception) {
        // ignore
    }

    return listOf(basicInfo, systemInfo, screenInfo, cpuInfo)
}

abstract class DeviceInfoSection(val name: String) {
    protected val mutableItems: MutableList<DeviceInfoItem> = mutableListOf()
    val items: List<DeviceInfoItem> get() = mutableItems
    abstract fun handle(key: String, value: String): Boolean
    fun addItem(name: String, value: String): Boolean =
        mutableItems.add(DeviceInfoItem(name, value))

    class BasicInfo() : DeviceInfoSection("基本信息") {
        override fun handle(key: String, value: String): Boolean {
            return when {
                // 基本设备信息
                key.contains("ro.product.model") -> addItem("设备型号", value)
                key.contains("ro.product.brand") -> addItem("基本信息", value)
                key.contains("ro.product.manufacturer") -> addItem("制造商", value)
                key.contains("ro.product.device") -> addItem("设备代号", value)
                key.contains("ro.product.name") -> addItem("产品名称", value)
                key.contains("ro.serialno") -> addItem("序列号", value)
                key.contains("ro.product.board") -> addItem("主板", value)
                else -> false
            }
        }
    }

    class SystemInfo() : DeviceInfoSection("系统版本") {
        override fun handle(key: String, value: String): Boolean {
            return when {
                // 系统版本信息
                key.contains("ro.build.version.release") -> addItem("Android版本", value)
                key.contains("ro.build.version.sdk") -> addItem("SDK版本", value)
                key.contains("ro.build.id") -> addItem("构建ID", value)
                key.contains("ro.build.version.incremental") -> addItem("增量版本", value)
                key.contains("ro.build.version.security_patch") -> addItem("安全补丁级别", value)
                key.contains("ro.build.fingerprint") -> addItem("构建指纹", value)
                key.contains("ro.build.display.id") -> addItem("显示ID", value)
                key.contains("ro.build.type") -> addItem("构建类型", value)
                key.contains("ro.build.user") -> addItem("构建用户", value)
                key.contains("ro.build.host") -> addItem("构建主机", value)
                else -> false
            }
        }
    }

    class ScreenInfo : DeviceInfoSection("屏幕信息") {
        override fun handle(key: String, value: String): Boolean = false
    }

    data class CpuInfo(val cores: MutableList<CoreInfo> = mutableListOf()) :
        DeviceInfoSection("CPU信息") {
        override fun handle(key: String, value: String): Boolean {
            return when {
                // 基本设备信息
                key == "ro.product.cpu.abi" -> addItem("架构", value)
                else -> false
            }
        }

        fun addCores(cores: List<CoreInfo>) {
            this.cores.addAll(cores)
        }

        companion object {
            fun parseCores(cpuInfoShell: String): List<CoreInfo> {
                val cpuList = mutableListOf<CoreInfo>()
                var currentCpuInfo: CoreInfo? = null

                cpuInfoShell.lines().forEach { line ->
                    // 忽略空行
                    if (line.isBlank()) return@forEach

                    // 尝试分割键值对
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()

                        if (key == "processor") {
                            // 如果当前已经在处理一个 CPU 信息，则先保存它
                            currentCpuInfo?.let { cpuList.add(it) }
                            // 开始一个新的 CPU 信息块
                            currentCpuInfo = CoreInfo(processor = value.toIntOrNull())
                        } else {
                            // 将属性添加到当前 CPU 信息中
                            currentCpuInfo?.properties?.set(key, value)
                        }
                    } else {
                        // 如果一行不能被 ':' 分割成两个部分，可能是一个不完整的行或格式错误
                        // 这里可以选择忽略或者记录日志
                        println("Warning: Skipping malformed line: $line")
                    }
                }

                // 不要忘记添加最后一个 CPU 信息块
                currentCpuInfo?.let { cpuList.add(it) }

                return cpuList
            }
        }


        data class CoreInfo(
            var processor: Int? = null,
            val properties: MutableMap<String, String> = mutableMapOf()
        )
    }
}

data class DeviceInfoItem(val name: String, val value: String)
