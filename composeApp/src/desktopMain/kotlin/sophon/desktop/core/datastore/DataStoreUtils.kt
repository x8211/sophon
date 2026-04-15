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
import sophon.desktop.core.usage.FeatureUsageModel
import sophon.desktop.feature.deeplink.data.source.DeepLinkHistoryModel
import sophon.desktop.feature.i18n.data.source.I18nToolConfig
import sophon.desktop.feature.i18n.model.I18nProject
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

    val featureUsage: DataStore<FeatureUsageModel> by lazy {
        create("featureUsage.pb", FeatureUsageModel(), FeatureUsageModel.serializer())
    }

    val deepLinkHistory: DataStore<DeepLinkHistoryModel> by lazy {
        create("deepLink.pb", DeepLinkHistoryModel(), DeepLinkHistoryModel.serializer())
    }

    val i18nTool: DataStore<I18nToolConfig> by lazy {
        create("i18n.pb", I18nToolConfig(), I18nToolConfig.serializer())
    }

    val i18nProject: DataStore<I18nProject> by lazy {
        create("project.pb", I18nProject(), I18nProject.serializer())
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
