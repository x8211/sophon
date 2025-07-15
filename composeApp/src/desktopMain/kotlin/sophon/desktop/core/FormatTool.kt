package sophon.desktop.core

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FormatTool {
    /**
     * 将KB值转换为合适的单位（KB、MB、GB）
     * @param sizeInKB 以KB为单位的大小字符串
     * @param decimals 保留小数位数
     * @return 格式化后的字符串，如"1.5 MB"
     */
    fun formatMemorySize(sizeInKB: String, decimals: Int = 0): String {
        try {
            val kb = sizeInKB.toLong()
            return formatMemorySize(kb, decimals)
        } catch (e: NumberFormatException) {
            // 如果转换失败，返回原始字符串
            return sizeInKB
        }
    }

    /**
     * 将KB值转换为合适的单位（KB、MB、GB）
     * @param sizeInKB 以KB为单位的大小
     * @param decimals 保留小数位数
     * @return 格式化后的字符串，如"1.5 MB"
     */
    fun formatMemorySize(sizeInKB: Long, decimals: Int = 2): String {
        if (sizeInKB <= 0) return "0 KB"
        
        val units = arrayOf("KB", "MB", "GB", "TB")
        val digitGroups = (log10(sizeInKB.toDouble()) / log10(1024.0)).toInt()
        
        // 确保不超出单位数组范围
        val unitIndex = digitGroups.coerceAtMost(units.size - 1)
        
        // 创建格式化对象
        val df = DecimalFormat()
        df.maximumFractionDigits = decimals
        df.minimumFractionDigits = 0 // 如果是整数则不显示小数部分
        
        // 计算并格式化
        val size = sizeInKB / 1024.0.pow(unitIndex.toDouble())
        return "${df.format(size)} ${units[unitIndex]}"
    }
} 