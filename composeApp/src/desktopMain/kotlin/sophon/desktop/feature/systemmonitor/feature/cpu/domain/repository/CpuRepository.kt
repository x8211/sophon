package sophon.desktop.feature.systemmonitor.feature.cpu.domain.repository

import sophon.desktop.feature.systemmonitor.feature.cpu.domain.model.CpuData

/**
 * CPU监测数据仓库接口
 */
interface CpuRepository {
    /**
     * 获取CPU监测数据
     * @return CPU监测数据
     */
    suspend fun getCpuData(): CpuData
}
