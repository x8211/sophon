package sophon.desktop.core.usage

import kotlinx.serialization.Serializable

@Serializable
data class FeatureUsageModel(val usages: Map<String, Int> = emptyMap())
