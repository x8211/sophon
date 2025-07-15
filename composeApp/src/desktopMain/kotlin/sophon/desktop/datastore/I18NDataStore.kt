package sophon.desktop.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import sophon.desktop.core.PB_HOME
import sophon.pb.desktop.I18N
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val i18nDataStore = DataStoreFactory.create(
    serializer = I18NDataStoreSerializer(),
    produceFile = { File("${PB_HOME}/i18n.pb") }
)

class I18NDataStoreSerializer : Serializer<I18N> {

    override val defaultValue: I18N = I18N()

    override suspend fun readFrom(input: InputStream): I18N {
        try {
            return I18N.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: I18N, output: OutputStream) = t.writeTo(output)
}

