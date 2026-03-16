package sophon.desktop.feature.systemmonitor.feature.camera.domain.repository

import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraData

/**
 * 相机监控数据仓库接口
 *
 * 定义获取相机服务状态数据的抽象接口
 */
interface CameraRepository {

    /**
     * 获取相机监控数据
     *
     * @return 相机数据汇总，包含全局信息、活跃客户端、事件日志和设备动态信息
     */
    suspend fun getCameraData(): CameraData
}
