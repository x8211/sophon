package sophon.desktop.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import sophon.desktop.core.PB_HOME
import sophon.desktop.pb.DeepLinkHistory
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val deepLinkDataStore = DataStoreFactory.create(
    serializer = DeepLinkDataStoreSerializer(),
    produceFile = { File("${PB_HOME}/deeplink.pb") }
)

class DeepLinkDataStoreSerializer : Serializer<DeepLinkHistory> {

    override val defaultValue: DeepLinkHistory = DeepLinkHistory()

    override suspend fun readFrom(input: InputStream): DeepLinkHistory {
        try {
            return DeepLinkHistory.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: DeepLinkHistory, output: OutputStream) = t.writeTo(output)
}
