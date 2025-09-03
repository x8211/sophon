package sophon.desktop

import sophon.desktop.feature.device.parseGetProp
import kotlin.test.Test
import java.io.File

/**
 * 设备信息解析器测试类
 */
class DeviceInfoParserTest {

    @Test
    fun getPropParserTest(){
        // 1. 读取getprop.txt文件内容
        val resourceUrl = javaClass.classLoader.getResource("getprop.txt")
            ?: throw IllegalArgumentException("测试资源文件未找到")
        val file = File(resourceUrl.toURI())
        val content = file.readText()
        
        // 2. 解析设备信息
        val deviceInfo = parseGetProp(content)
        
        // 3. 打印解析结果
        println("=== Android设备信息解析结果 ===")
        println(deviceInfo)
    }

}