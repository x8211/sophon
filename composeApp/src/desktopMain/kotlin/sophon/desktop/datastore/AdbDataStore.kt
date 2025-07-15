package sophon.desktop.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import sophon.desktop.core.PB_HOME
import sophon.pb.desktop.Adb
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val adbDataStore = DataStoreFactory.create(
    serializer = AdbDataStoreSerializer(),
    produceFile = { File("${PB_HOME}/adb.pb") }
)

class AdbDataStoreSerializer : Serializer<Adb> {

    override val defaultValue: Adb = Adb()

    override suspend fun readFrom(input: InputStream): Adb {
        try {
            return Adb.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Adb, output: OutputStream) = t.writeTo(output)
}

