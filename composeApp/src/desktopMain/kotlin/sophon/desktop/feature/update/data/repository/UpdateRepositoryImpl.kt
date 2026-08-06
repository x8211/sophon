package sophon.desktop.feature.update.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import sophon.desktop.feature.update.model.UpdateInfo
import sophon.desktop.generated.AppInfo
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 版本信息 JSON 文件的远程地址，修改此处以指向实际部署的服务器。
 * 格式参见 [UpdateInfo] 的 KDoc。
 */
private const val UPDATE_CHECK_URL =
    "https://raw.githubusercontent.com/x8211/sophon/master/version.json"

private val json = Json { ignoreUnknownKeys = true }

class UpdateRepositoryImpl : UpdateRepository {

    override suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val responseText = fetchText(UPDATE_CHECK_URL)
        val info = json.decodeFromString<UpdateInfo>(responseText)
        if (isNewerVersion(current = AppInfo.APP_VERSION, latest = info.version)) info else null
    }

    override fun downloadUpdate(info: UpdateInfo): Flow<DownloadState> = channelFlow {
        val os = System.getProperty("os.name").lowercase()
        val url = when {
            os.contains("mac") -> info.downloadUrl.macos
            os.contains("win") -> info.downloadUrl.windows
            else -> info.downloadUrl.macos
        }

        if (url.isBlank()) {
            send(DownloadState.Error("当前平台暂无可用的下载链接"))
            return@channelFlow
        }

        val fileName = url.substringAfterLast("/").ifBlank { "Sophon-${info.version}.pkg" }
        val tempFile = File(updateDownloadDir(), fileName)

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 60_000
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            send(DownloadState.Error("下载失败：HTTP ${connection.responseCode}"))
            return@channelFlow
        }

        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L
        val buffer = ByteArray(8 * 1024)

        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0) {
                        send(DownloadState.Progress(downloadedBytes.toFloat() / totalBytes))
                    }
                }
            }
        }

        send(DownloadState.Complete(tempFile))
    }.flowOn(Dispatchers.IO)

    override suspend fun cleanupDownloadedUpdates() {
        withContext(Dispatchers.IO) {
            updateDownloadDir().listFiles()?.forEach { it.delete() }
        }
    }

    private fun updateDownloadDir(): File =
        File(System.getProperty("java.io.tmpdir"), "sophon-updates").also { it.mkdirs() }

    private fun fetchText(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 10_000
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP ${connection.responseCode}")
        }
        return connection.inputStream.bufferedReader().readText()
    }

    /**
     * 语义版本号比较，判断 [latest] 是否比 [current] 更新。
     * 仅支持 MAJOR.MINOR.PATCH 格式，忽略非数字部分。
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val cur = current.split(".").mapNotNull { it.toIntOrNull() }
        val new = latest.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(cur.size, new.size)) {
            val c = cur.getOrElse(i) { 0 }
            val n = new.getOrElse(i) { 0 }
            if (n > c) return true
            if (n < c) return false
        }
        return false
    }
}
