package sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.repository

import sophon.desktop.feature.systemmonitor.feature.cpu.common.domain.model.ThreadCpuInfo
import sophon.desktop.feature.systemmonitor.feature.cpu.realtime.domain.model.RealtimeCpuData

/**
 * 实时CPU监测数据仓库接口
 */
interface RealtimeCpuRepository {
    /**
     * 获取实时CPU监测数据
     * 使用 top -n 1 -b -m 10 命令获取当前瞬时的CPU使用情况
     * @return 实时CPU监测数据
     */
    suspend fun getRealtimeCpuData(): RealtimeCpuData
    
    /**
     * 获取指定进程的所有线程CPU使用信息
     * @param pid 进程ID
     * @return 线程CPU使用信息列表
     */
    suspend fun getProcessThreads(pid: Int): List<ThreadCpuInfo>
}
