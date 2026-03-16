package sophon.desktop.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import sophon.desktop.core.PB_HOME
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * 通用的 DataStore 序列化器实现类，使用 kotlinx-serialization-json
 */
@OptIn(ExperimentalSerializationApi::class)
class JsonSerializer<T>(
    override val defaultValue: T,
    private val serializer: kotlinx.serialization.KSerializer<T>
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T {
        try {
            return Json.decodeFromStream(serializer, input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read data.", exception)
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        Json.encodeToStream(serializer, t, output)
    }
}

/**
 * 创建 DataStore 的工厂扩展方法
 */
inline fun <reified T> createDataStore(
    fileName: String,
    defaultValue: T,
    serializer: kotlinx.serialization.KSerializer<T>
): DataStore<T> {
    return DataStoreFactory.create(
        serializer = JsonSerializer(defaultValue, serializer),
        produceFile = { File("$PB_HOME/$fileName") }
    )
}
