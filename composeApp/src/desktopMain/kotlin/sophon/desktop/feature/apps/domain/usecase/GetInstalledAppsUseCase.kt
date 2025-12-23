package sophon.desktop.feature.apps.domain.usecase

import kotlinx.coroutines.flow.Flow
import sophon.desktop.feature.apps.domain.model.AppLoadState
import sophon.desktop.feature.apps.domain.repository.InstalledAppsRepository

class GetInstalledAppsUseCase(private val repository: InstalledAppsRepository) {
    operator fun invoke(): Flow<AppLoadState> {
        return repository.getInstalledApps()
    }
}
