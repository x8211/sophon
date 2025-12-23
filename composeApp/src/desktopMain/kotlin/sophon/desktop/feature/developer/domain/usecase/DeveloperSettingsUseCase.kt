package sophon.desktop.feature.developer.domain.usecase

import sophon.desktop.feature.developer.domain.model.DeveloperOptions
import sophon.desktop.feature.developer.domain.repository.DeveloperRepository

class DeveloperSettingsUseCase(private val repository: DeveloperRepository) {

    suspend fun getOptions(): DeveloperOptions = repository.getOptions()

    suspend fun toggleDebugLayout(current: Boolean) = repository.setDebugLayout(!current)
    
    suspend fun toggleHwUi(current: Boolean) = repository.setHwUi(!current)
    
    suspend fun toggleShowTouches(current: Boolean) = repository.setShowTouches(!current)
    
    suspend fun togglePointerLocation(current: Boolean) = repository.setPointerLocation(!current)
    
    suspend fun toggleStrictMode(current: Boolean) = repository.setStrictMode(!current)
    
    suspend fun toggleForceRtl(current: Boolean) = repository.setForceRtl(!current)
    
    suspend fun toggleStayAwake(current: Boolean) = repository.setStayAwake(!current)
    
    suspend fun toggleShowAllANRs(current: Boolean) = repository.setShowAllANRs(!current)
    
    suspend fun toggleDontKeepActivities(current: Boolean) = repository.setDontKeepActivities(!current)

    // 对于 Scale 类，通常在 UI 也是 toggle 或者选择，这里保留 set 接口更灵活
    suspend fun setWindowAnimationScale(scale: Float) = repository.setWindowAnimationScale(scale)
    suspend fun setTransitionAnimationScale(scale: Float) = repository.setTransitionAnimationScale(scale)
    suspend fun setAnimatorDurationScale(scale: Float) = repository.setAnimatorDurationScale(scale)
}
