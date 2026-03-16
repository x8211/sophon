package sophon.desktop.feature.developer.domain.repository

import sophon.desktop.feature.developer.domain.model.DeveloperOptions

interface DeveloperRepository {
    suspend fun getOptions(): DeveloperOptions
    
    suspend fun setDebugLayout(enabled: Boolean)
    suspend fun setHwUi(enabled: Boolean)
    suspend fun setShowTouches(enabled: Boolean)
    suspend fun setPointerLocation(enabled: Boolean)
    suspend fun setStrictMode(enabled: Boolean)
    suspend fun setForceRtl(enabled: Boolean)
    suspend fun setStayAwake(enabled: Boolean)
    suspend fun setShowAllANRs(enabled: Boolean)
    suspend fun setDontKeepActivities(enabled: Boolean)
    
    suspend fun setWindowAnimationScale(scale: Float)
    suspend fun setTransitionAnimationScale(scale: Float)
    suspend fun setAnimatorDurationScale(scale: Float)
}
