package sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.repository

import sophon.desktop.feature.systemmonitor.feature.cpu.common.domain.model.ThreadCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys.domain.model.CpuData

/**
 * CPU监测数据仓库接口
 */
interface CpuRepository {
    /**
     * 获取CPU监测数据
     * @return CPU监测数据
     */
    suspend fun getCpuData(): CpuData
    
    /**
     * 获取指定进程的所有线程CPU使用信息
     * @param pid 进程ID
     * @return 线程CPU使用信息列表
     */
    suspend fun getProcessThreads(pid: Int): List<ThreadCpuInfo>
}
