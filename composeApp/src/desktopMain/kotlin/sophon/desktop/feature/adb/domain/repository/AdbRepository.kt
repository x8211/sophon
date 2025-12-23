package sophon.desktop.feature.adb.domain.repository

import kotlinx.coroutines.flow.StateFlow
import sophon.desktop.feature.adb.domain.model.AdbState

/**
 * ADB 仓库接口，定义 ADB 相关数据的操作
 */
interface AdbRepository {
    /**
     * 获取 ADB 状态流 (StateFlow 支持同步获取当前值)
     */
    fun getAdbState(): StateFlow<AdbState>

    /**
     * 更新 ADB 路径
     */
    suspend fun updateAdbPath(path: String)

    /**
     * 选择设备
     */
    suspend fun selectDevice(deviceName: String)

    /**
     * 手动刷新设备列表
     */
    suspend fun refreshDevices()

    /**
     * 自动寻找 ADB 工具
     */
    suspend fun autoFindAdbTool(): String?
}
