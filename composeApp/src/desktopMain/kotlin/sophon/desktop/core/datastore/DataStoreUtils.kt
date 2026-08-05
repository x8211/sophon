package sophon.desktop.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import sophon.desktop.core.CACHE_HOME
import sophon.desktop.feature.deeplink.data.source.DeepLinkHistoryModel
import sophon.desktop.feature.installaab.data.source.AabKeystoreCache
import java.io.File
import java.io.InputStream
import java.io.OutputStream

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
 * 全局唯一的 DataStore 提供者，通过 object 单例保证每个文件只有一个 DataStore 实例。
 * 避免 ProGuard 混淆/优化后顶层 val 的 clinit 被多次触发导致重复创建。
 */
object DataStoreProvider {

    val deepLinkHistory: DataStore<DeepLinkHistoryModel> by lazy {
        create("deepLink.pb", DeepLinkHistoryModel(), DeepLinkHistoryModel.serializer())
    }

    val aabKeystoreCache: DataStore<AabKeystoreCache> by lazy {
        create("aabKeystore.pb", AabKeystoreCache(), AabKeystoreCache.serializer())
    }

    private fun <T> create(
        fileName: String,
        defaultValue: T,
        serializer: kotlinx.serialization.KSerializer<T>
    ): DataStore<T> {
        return DataStoreFactory.create(
            serializer = JsonSerializer(defaultValue, serializer),
            produceFile = { File("$CACHE_HOME/$fileName") }
        )
    }
}
