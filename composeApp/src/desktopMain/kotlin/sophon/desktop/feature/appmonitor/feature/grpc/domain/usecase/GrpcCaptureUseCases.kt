package sophon.desktop.feature.appmonitor.feature.grpc.domain.usecase

import sophon.desktop.feature.appmonitor.feature.grpc.domain.model.GrpcCaptureModel
import sophon.desktop.feature.appmonitor.feature.grpc.domain.repository.GrpcCaptureRepository

/**
 * 从本地缓存数据库中获取 gRPC 捕获记录的用例
 *
 * @param repository gRPC 捕获仓库
 */
class GetGrpcCaptureUseCase(private val repository: GrpcCaptureRepository) {
    /**
     * 执行用例，返回已缓存的 gRPC 捕获记录列表
     *
     * @return 捕获到的 gRPC 记录列表
     */
    suspend operator fun invoke(): List<GrpcCaptureModel> {
        return repository.getCapturedRecords()
    }
}

/**
 * 通过 run-as 命令拉取目标应用数据库的用例
 *
 * @param repository gRPC 捕获仓库
 */
class RefreshGrpcCaptureUseCase(private val repository: GrpcCaptureRepository) {
    /**
     * 执行用例，拉取指定包名应用的 Protodroid.db 到本地
     *
     * @param packageName 目标应用包名
     * @return 是否拉取成功
     */
    suspend operator fun invoke(packageName: String): Boolean {
        return repository.refreshDatabase(packageName)
    }
}
