package sophon.desktop.core.usage

import kotlinx.serialization.Serializable
import sophon.desktop.core.datastore.createDataStore

@Serializable
data class FeatureUsageModel(val usages: Map<String, Int> = emptyMap())

val featureUsageDataStore = createDataStore(
    fileName = "featureUsage.pb",
    defaultValue = FeatureUsageModel(),
    serializer = FeatureUsageModel.serializer()
)
