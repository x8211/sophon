package sophon.desktop.feature.appmonitor.feature.grpc.data.repository

import sophon.desktop.feature.appmonitor.feature.grpc.model.GrpcCaptureModel

/**
 * gRPC 捕获仓库接口
 *
 * 提供从目标 Android 应用拉取 Protodroid.db 并解析 gRPC 记录的能力
 */
interface GrpcCaptureRepository {
    /**
     * 获取本地缓存数据库中捕获到的记录列表
     *
     * @return 捕获到的 gRPC 记录列表
     */
    suspend fun getCapturedRecords(): List<GrpcCaptureModel>

    /**
     * 通过 run-as 命令从设备拉取数据库到本地 PB_HOME 目录
     *
     * @param packageName 目标应用包名，用于 run-as 权限和数据库路径定位
     * @return 是否拉取成功
     */
    suspend fun refreshDatabase(packageName: String): Boolean
}
