package sophon.pb.desktop

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class Project(val absolutePath: String = "", val modules: List<Module> = emptyList()) {
    @OptIn(ExperimentalSerializationApi::class)
    fun writeTo(outputStream: OutputStream) {
        Json.encodeToStream(this, outputStream)
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        @JvmStatic
        fun parseFrom(input: InputStream): Project {
            return Json.decodeFromStream(input)
        }
    }
}

@Serializable
data class Module(val name: String = "", val level: Int = 0, val absolutePath: String = "") {
    @OptIn(ExperimentalSerializationApi::class)
    fun writeTo(outputStream: OutputStream) {
        Json.encodeToStream(this, outputStream)
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        @JvmStatic
        fun parseFrom(input: InputStream): Module {
            return Json.decodeFromStream(input)
        }
    }
}
