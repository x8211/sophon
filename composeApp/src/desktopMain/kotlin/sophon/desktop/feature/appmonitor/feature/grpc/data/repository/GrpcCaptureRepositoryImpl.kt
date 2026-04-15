package sophon.desktop.feature.appmonitor.feature.grpc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sophon.desktop.core.PB_HOME
import sophon.desktop.core.Shell.byteStreamShell
import sophon.desktop.feature.appmonitor.feature.grpc.model.GrpcCaptureModel
import java.io.File
import java.sql.DriverManager

/**
 * gRPC 捕获仓库实现类
 *
 * 核心流程：
 * 1. 使用 `adb shell run-as <packageName> cat databases/Protodroid.db` 将数据库
 *    以 stdout 方式输出，并通过进程重定向写入本地 PB_HOME 目录。
 * 2. 读取本地 SQLite 文件，查询 ProtodroidDataEntity 表返回记录列表。
 *
 * 注意：run-as 命令要求目标应用为 debuggable 版本。
 */
class GrpcCaptureRepositoryImpl : GrpcCaptureRepository {

    private val dbName = "Protodroid.db"

    private val localDbFile = File(PB_HOME, dbName)

    // -------------------------------------------------------------------
    // 读取记录
    // -------------------------------------------------------------------

    /**
     * 从本地已缓存的 SQLite 数据库中查询 gRPC 捕获记录
     *
     * 列名来自 Protodroid 源码 ProtodroidDataEntity：
     * id, service_url, service_name, request_header, response_header,
     * request_body, response_body, status_code, status_level,
     * status_name, status_desc, status_error_cause, create_timestamp, update_timestamp
     *
     * @return ProtodroidDataEntity 表中最近 100 条记录，按 create_timestamp 倒序
     */
    override suspend fun getCapturedRecords(): List<GrpcCaptureModel> {
        println("[GrpcCapture] >>> getCapturedRecords() started")
        println("[GrpcCapture] Local database path: ${localDbFile.absolutePath}")

        if (!localDbFile.exists()) {
            println("[GrpcCapture] ✗ Local database file does not exist, please refresh first")
            return emptyList()
        }

        println("[GrpcCapture] ✓ Local database file exists, size: ${localDbFile.length()} bytes")

        val records = mutableListOf<GrpcCaptureModel>()
        return withContext(Dispatchers.IO) {
            try {
                Class.forName("org.sqlite.JDBC")
                println("[GrpcCapture] ✓ SQLite JDBC driver loaded successfully")

                DriverManager.getConnection("jdbc:sqlite:${localDbFile.absolutePath}")
                    .use { connection ->
                        println("[GrpcCapture] ✓ Database connection established")

                        val stmt = connection.createStatement()

                        val allTables = mutableListOf<String>()
                        val tableRs = stmt.executeQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
                        )
                        while (tableRs.next()) {
                            allTables.add(tableRs.getString("name"))
                        }
                        println("[GrpcCapture] ✓ All tables (${allTables.size}): ${allTables.joinToString()}")

                        val pragmaRs = connection.createStatement()
                            .executeQuery("PRAGMA table_info(ProtodroidDataEntity)")
                        val columns = mutableListOf<String>()
                        while (pragmaRs.next()) {
                            val colName = pragmaRs.getString("name")
                            val colType = pragmaRs.getString("type")
                            columns.add(colName)
                            println("[GrpcCapture]   Column: $colName ($colType)")
                        }
                        println("[GrpcCapture] ✓ Table ProtodroidDataEntity has ${columns.size} columns: ${columns.joinToString()}")

                        val sql =
                            "SELECT * FROM ProtodroidDataEntity ORDER BY create_timestamp DESC LIMIT 100"
                        println("[GrpcCapture] Executing query: $sql")

                        val resultSet = connection.createStatement().executeQuery(sql)
                        var count = 0
                        while (resultSet.next()) {
                            records.add(
                                GrpcCaptureModel(
                                    id = runCatching { resultSet.getLong("id") }.getOrDefault(0L),
                                    serviceUrl = runCatching { resultSet.getString("service_url") }.getOrDefault(""),
                                    serviceName = runCatching { resultSet.getString("service_name") }.getOrDefault(""),
                                    requestHeader = runCatching { resultSet.getString("request_header") }.getOrDefault(""),
                                    responseHeader = runCatching { resultSet.getString("response_header") }.getOrDefault(""),
                                    requestBody = runCatching { resultSet.getString("request_body") }.getOrDefault(""),
                                    responseBody = runCatching { resultSet.getString("response_body") }.getOrDefault(""),
                                    statusCode = runCatching { resultSet.getInt("status_code") }.getOrDefault(-1),
                                    statusLevel = runCatching { resultSet.getInt("status_level") }.getOrDefault(0),
                                    statusName = runCatching { resultSet.getString("status_name") }.getOrDefault(""),
                                    statusDesc = runCatching { resultSet.getString("status_desc") }.getOrDefault(""),
                                    statusErrorCause = runCatching { resultSet.getString("status_error_cause") }.getOrDefault(""),
                                    createTimestamp = runCatching { resultSet.getLong("create_timestamp") }.getOrDefault(0L),
                                    updateTimestamp = runCatching { resultSet.getLong("update_timestamp") }.getOrDefault(0L)
                                )
                            )
                            count++
                        }
                        println("[GrpcCapture] ✓ Query complete, fetched $count records")
                    }
                records
            } catch (e: Exception) {
                println("[GrpcCapture] ✗ Failed to read database: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // -------------------------------------------------------------------
    // 刷新数据库（核心：run-as 拉取）
    // -------------------------------------------------------------------

    /**
     * 通过 `run-as` 命令将目标应用的 Protodroid.db 拉取到本地 PB_HOME 目录
     *
     * 执行流程：
     * 1. 确保本地缓存目录存在
     * 2. 用 run-as 将 databases/Protodroid.db 以 stdout 模式输出并写入本地文件
     * 3. 校验本地文件是否写入成功
     *
     * @param packageName 目标应用包名（从当前监控的前台应用自动获取）
     * @return true 表示文件拉取并写入成功
     */
    override suspend fun refreshDatabase(packageName: String): Boolean {
        println("[GrpcCapture] >>> refreshDatabase() started, target package: $packageName")

        val parentDir = localDbFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
            println("[GrpcCapture] ✓ Created local cache directory: ${parentDir.absolutePath}")
        } else {
            println("[GrpcCapture] ✓ Local cache directory already exists: ${parentDir?.absolutePath}")
        }

        if (localDbFile.exists()) {
            val deleted = localDbFile.delete()
            println("[GrpcCapture] ${if (deleted) "✓" else "✗"} Deleted old database file: ${localDbFile.absolutePath}")
        }

        return try {
            val rawCmd = "adb shell run-as $packageName cat databases/$dbName"
            println("[GrpcCapture] Executing command: $rawCmd")

            println("[GrpcCapture] Writing adb stdout to local file...")
            var bytesWritten = 0L
            localDbFile.outputStream().use { outputStream ->
                rawCmd.byteStreamShell().collect { chunk ->
                    withContext(Dispatchers.IO) {
                        outputStream.write(chunk)
                        bytesWritten += chunk.size
                    }
                }
            }
            println("[GrpcCapture] ✓ Write complete, total $bytesWritten bytes written")

            val fileExists = localDbFile.exists()
            val fileSize = if (fileExists) localDbFile.length() else 0L

            println("[GrpcCapture] Local file validation → exists: $fileExists | size: $fileSize bytes")

            val success = fileExists && fileSize > 100L
            if (success) {
                println("[GrpcCapture] ✓ Database pull succeeded! Path: ${localDbFile.absolutePath}")
            } else {
                println("[GrpcCapture] ✗ Database pull failed: file does not exist or is empty")
                if (fileExists) localDbFile.delete()
            }
            success
        } catch (e: Exception) {
            println("[GrpcCapture] ✗ refreshDatabase exception: ${e.message}")
            e.printStackTrace()
            false
        }

    }
}
