package sophon.desktop.feature.systemmonitor.data.repository

import sophon.desktop.feature.systemmonitor.domain.repository.SystemMonitorRepository

/**
 * 系统监控仓库实现
 *
 * 实现系统信息获取逻辑
 */
class SystemMonitorRepositoryImpl : SystemMonitorRepository {

    /**
     * 当前时间戳
     */
    override suspend fun getTimestamp(): Long = System.currentTimeMillis()
}
