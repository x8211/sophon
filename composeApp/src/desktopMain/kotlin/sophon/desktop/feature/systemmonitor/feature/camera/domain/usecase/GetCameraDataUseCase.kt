package sophon.desktop.feature.systemmonitor.feature.camera.domain.usecase

import sophon.desktop.feature.systemmonitor.feature.camera.domain.model.CameraData
import sophon.desktop.feature.systemmonitor.feature.camera.domain.repository.CameraRepository

/**
 * 获取相机监控数据用例
 *
 * 领域层用例，封装获取相机监控数据的业务逻辑
 *
 * @param repository 相机数据仓库
 */
class GetCameraDataUseCase(
    private val repository: CameraRepository
) {
    /**
     * 执行用例，获取相机监控数据
     *
     * @return 相机数据汇总
     */
    suspend operator fun invoke(): CameraData {
        return repository.getCameraData()
    }
}
