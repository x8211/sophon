package sophon.desktop.feature.systemmonitor.domain.repository

/**
 * 系统监控仓库接口
 *
 * 定义获取系统信息的方法
 */
interface SystemMonitorRepository {

    /**
     * 获取当前时间戳
     */
    suspend fun getTimestamp(): Long
}
