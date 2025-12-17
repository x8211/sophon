package sophon.desktop.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import sophon.desktop.core.PB_HOME
import sophon.desktop.pb.FeatureUsage
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val featureUsageDataStore = DataStoreFactory.create(
    serializer = FeatureUsageDataStoreSerializer(),
    produceFile = { File("${PB_HOME}/feature_usage.pb") }
)

class FeatureUsageDataStoreSerializer : Serializer<FeatureUsage> {

    override val defaultValue: FeatureUsage = FeatureUsage()

    override suspend fun readFrom(input: InputStream): FeatureUsage {
        try {
            return FeatureUsage.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: FeatureUsage, output: OutputStream) = t.writeTo(output)
}
