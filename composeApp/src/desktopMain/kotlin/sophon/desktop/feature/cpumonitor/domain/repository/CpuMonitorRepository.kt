package sophon.desktop.feature.cpumonitor.domain.repository

import sophon.desktop.feature.cpumonitor.domain.model.CpuMonitorData

/**
 * CPU监测数据仓库接口
 */
interface CpuMonitorRepository {
    /**
     * 获取CPU监测数据
     * @param packageName 包名,如果为null则获取所有进程的CPU信息
     * @return CPU监测数据
     */
    suspend fun getCpuMonitorData(packageName: String?): CpuMonitorData
}
