package sophon.desktop.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import sophon.desktop.core.PB_HOME
import sophon.desktop.pb.Project
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val projectDataStore = DataStoreFactory.create(
    serializer = ProjectDataStoreSerializer(),
    produceFile = { File("${PB_HOME}/project.pb") }
)

class ProjectDataStoreSerializer : Serializer<Project> {

    override val defaultValue: Project = Project()

    override suspend fun readFrom(input: InputStream): Project {
        try {
            return Project.parseFrom(input)
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Project, output: OutputStream) = t.writeTo(output)
}

