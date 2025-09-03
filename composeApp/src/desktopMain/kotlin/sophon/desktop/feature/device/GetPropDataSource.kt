package sophon.desktop.feature.device

import sophon.desktop.core.Shell.simpleShell

suspend fun getProp(): String = "adb shell getprop".simpleShell()

fun parseGetProp(content: String): List<DeviceInfoSection> {
    // 初始化各个分类
    val section1 = DeviceInfoSection("基本信息")
    val section2 = DeviceInfoSection("系统版本")
    val section3 = DeviceInfoSection("硬件信息")
    val section4 = DeviceInfoSection("网络信息")
    val section5 = DeviceInfoSection("安全信息")
    val section6 = DeviceInfoSection("性能配置")
    val section7 = DeviceInfoSection("其他信息")

    // 解析每一行
    content.lines().forEach { line ->
        val trimmedLine = line.trim()
        if (trimmedLine.startsWith("[") && trimmedLine.contains("]: [")) {
            val regex = "\\[([^]]+)]: \\[([^]]*)]".toRegex()
            val matchResult = regex.find(trimmedLine)
            if (matchResult == null) return@forEach

            val key = matchResult.groupValues[1]
            val value = matchResult.groupValues[2]

            when {
                // 基本设备信息
                key.contains("ro.product.model") -> section1.addItem("设备型号", value)
                key.contains("ro.product.brand") -> section1.addItem("基本信息", value)
                key.contains("ro.product.manufacturer") -> section1.addItem("制造商", value)
                key.contains("ro.product.device") -> section1.addItem("设备代号", value)
                key.contains("ro.product.name") -> section1.addItem("产品名称", value)
                key.contains("ro.serialno") -> section1.addItem("序列号", value)
                key.contains("ro.product.board") -> section1.addItem("主板", value)

                // 系统版本信息
                key.contains("ro.build.version.release") -> section2.addItem("Android版本", value)
                key.contains("ro.build.version.sdk") -> section2.addItem("SDK版本", value)
                key.contains("ro.build.id") -> section2.addItem("构建ID", value)
                key.contains("ro.build.version.incremental") -> section2.addItem("增量版本", value)
                key.contains("ro.build.version.security_patch") ->
                    section2.addItem("安全补丁级别", value)

                key.contains("ro.build.fingerprint") -> section2.addItem("构建指纹", value)
                key.contains("ro.build.display.id") -> section2.addItem("显示ID", value)
                key.contains("ro.build.type") -> section2.addItem("构建类型", value)
                key.contains("ro.build.user") -> section2.addItem("构建用户", value)
                key.contains("ro.build.host") -> section2.addItem("构建主机", value)

                // 硬件信息
                key.contains("ro.product.cpu.abi") -> section3.addItem("CPU架构", value)
                key.contains("ro.product.cpu.abilist") -> section3.addItem("支持的ABI列表", value)
                key.contains("ro.product.cpu.abilist32") -> section3.addItem("32位ABI列表", value)
                key.contains("ro.product.cpu.abilist64") -> section3.addItem("64位ABI列表", value)
                key.contains("ro.hardware") -> section3.addItem("硬件平台", value)
                key.contains("ro.board.platform") -> section3.addItem("主板平台", value)
                key.contains("ro.opengles.version") -> section3.addItem("OpenGL ES版本", value)
                key.contains("ro.cpuvulkan.version") -> section3.addItem("Vulkan版本", value)
                key.contains("qemu.sf.lcd_density") || key.contains("ro.sf.lcd_density") ->
                    section3.addItem("屏幕密度", value)

                // 网络信息
                key.contains("gsm.operator.alpha") -> section4.addItem("运营商名称", value)
                key.contains("gsm.operator.numeric") -> section4.addItem("运营商代码", value)
                key.contains("gsm.operator.iso-country") -> section4.addItem("国家代码", value)
                key.contains("gsm.network.type") -> section4.addItem("网络类型", value)
                key.contains("gsm.sim.state") -> section4.addItem("SIM卡状态", value)
                key.contains("gsm.version.baseband") -> section4.addItem("基带版本", value)
                key.contains("net.bt.name") -> section4.addItem("蓝牙名称", value)

                // 安全信息
                key.contains("ro.secure") ->
                    section5.addItem("安全模式", if (value == "1") "已启用" else "已禁用")

                key.contains("ro.adb.secure") ->
                    section5.addItem("ADB安全模式", if (value == "1") "已启用" else "已禁用")

                key.contains("ro.debuggable") ->
                    section5.addItem("可调试", if (value == "1") "已启用" else "已禁用")

                key.contains("ro.allow.mock.location") ->
                    section5.addItem("允许模拟位置", if (value == "1") "已启用" else "已禁用")

                key.contains("ro.crypto.state") ->
                    section5.addItem("加密状态", if (value == "1") "已启用" else "已禁用")

                // 性能配置
                key.contains("dalvik.vm.heapsize") -> section6.addItem("堆大小", value)
                key.contains("dalvik.vm.dex2oat-Xms") -> section6.addItem("Dex2oat最小内存", value)
                key.contains("dalvik.vm.dex2oat-Xmx") -> section6.addItem("Dex2oat最大内存", value)
                key.contains("ro.kernel.version") -> section6.addItem("内核版本", value)

                // 其他重要信息
                key.contains("persist.sys.locale") -> section7.addItem("系统语言", value)
                key.contains("persist.sys.timezone") -> section7.addItem("时区", value)
                key.contains("ro.boot.qemu.avd_name") -> section7.addItem("模拟器AVD名称", value)
                key.contains("ro.kernel.qemu") ->
                    section7.addItem("是否为模拟器", if (value == "1") "是" else "否")

                key.contains("sys.boot_completed") ->
                    section7.addItem("启动完成", if (value == "1") "是" else "否")

                key.contains("ro.treble.enabled") ->
                    section7.addItem("Treble支持", if (value == "1") "是" else "否")

                key.contains("ro.vndk.version") ->
                    section7.addItem("VNDK版本", if (value == "1") "是" else "否")
            }
        }
    }

    return listOf(section1, section2, section3, section4, section5, section6, section7)
}
