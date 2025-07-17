package sophon.desktop.pb

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class I18N(val toolPath: String = "") {

    @OptIn(ExperimentalSerializationApi::class)
    fun writeTo(outputStream: OutputStream) {
        Json.encodeToStream(this, outputStream)
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun parseFrom(input: InputStream): I18N {
            return Json.decodeFromStream(input)
        }
    }
}