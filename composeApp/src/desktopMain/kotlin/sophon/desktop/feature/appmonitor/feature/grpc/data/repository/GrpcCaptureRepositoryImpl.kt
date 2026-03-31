package sophon.desktop.feature.appmonitor.feature.grpc.data.repository

import sophon.desktop.core.PB_HOME
import sophon.desktop.feature.appmonitor.feature.grpc.domain.model.GrpcCaptureModel
import sophon.desktop.feature.appmonitor.feature.grpc.domain.repository.GrpcCaptureRepository
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

    /** 本地数据库文件名 */
    private val dbName = "Protodroid.db"

    /** 本地缓存目录下的数据库文件 */
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
        println("[GrpcCapture] >>> getCapturedRecords() 开始")
        println("[GrpcCapture] 本地数据库路径: ${localDbFile.absolutePath}")

        if (!localDbFile.exists()) {
            println("[GrpcCapture] ✗ 本地数据库文件不存在，请先执行刷新操作")
            return emptyList()
        }

        println("[GrpcCapture] ✓ 本地数据库文件存在，大小: ${localDbFile.length()} bytes")

        val records = mutableListOf<GrpcCaptureModel>()
        return try {
            Class.forName("org.sqlite.JDBC")
            println("[GrpcCapture] ✓ SQLite JDBC 驱动加载成功")

            DriverManager.getConnection("jdbc:sqlite:${localDbFile.absolutePath}").use { connection ->
                println("[GrpcCapture] ✓ 数据库连接建立成功")

                val stmt = connection.createStatement()

                // ── 打印所有表名（调试用） ──────────────────────────────────
                val allTables = mutableListOf<String>()
                val tableRs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
                )
                while (tableRs.next()) {
                    allTables.add(tableRs.getString("name"))
                }
                println("[GrpcCapture] ✓ 数据库所有表名(${allTables.size}): ${allTables.joinToString()}")

                // ── 打印目标表列信息（调试用） ────────────────────────────────
                val pragmaRs = connection.createStatement()
                    .executeQuery("PRAGMA table_info(ProtodroidDataEntity)")
                val columns = mutableListOf<String>()
                while (pragmaRs.next()) {
                    val colName = pragmaRs.getString("name")
                    val colType = pragmaRs.getString("type")
                    columns.add(colName)
                    println("[GrpcCapture]   列: $colName ($colType)")
                }
                println("[GrpcCapture] ✓ 表 ProtodroidDataEntity 共 ${columns.size} 列: ${columns.joinToString()}")

                // ── 执行查询 ──────────────────────────────────────────────────
                val sql = "SELECT * FROM ProtodroidDataEntity ORDER BY create_timestamp DESC LIMIT 100"
                println("[GrpcCapture] 执行查询: $sql")

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
                println("[GrpcCapture] ✓ 查询完成，共读取 $count 条记录")
            }
            records
        } catch (e: Exception) {
            println("[GrpcCapture] ✗ 读取数据库异常: ${e.message}")
            e.printStackTrace()
            emptyList()
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
        println("[GrpcCapture] >>> refreshDatabase() 开始，目标包名: $packageName")

        // 步骤 1：确保本地目录存在
        val parentDir = localDbFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
            println("[GrpcCapture] ✓ 创建本地缓存目录: ${parentDir.absolutePath}")
        } else {
            println("[GrpcCapture] ✓ 本地缓存目录已存在: ${parentDir?.absolutePath}")
        }

        // 步骤 2：清除旧文件
        if (localDbFile.exists()) {
            val deleted = localDbFile.delete()
            println("[GrpcCapture] ${if (deleted) "✓" else "✗"} 清除旧数据库文件: ${localDbFile.absolutePath}")
        }

        return try {
            // 步骤 3：使用 run-as 通过 ProcessBuilder 直接读取数据库二进制并写入本地
            // 等价命令：adb shell "run-as <pkg> cat databases/Protodroid.db" > localDbFile
            // ⚠️ 此处未使用 Shell 工具类（simpleShell/oneshotShell/streamShell），
            //    因为其返回值均为 String，无法处理二进制流 (SQLite .db 文件)。
            //    需要直接读取进程 InputStream 写入本地文件。
            val adbCmd = listOf(
                "adb", "shell",
                "run-as $packageName cat databases/$dbName"
            )
            println("[GrpcCapture] 执行命令: ${adbCmd.joinToString(" ")}")

            val process = ProcessBuilder(adbCmd)
                .redirectErrorStream(false)
                .start()

            // 步骤 4：将 stdout 写入本地文件
            println("[GrpcCapture] 开始从 adb stdout 写入本地文件...")
            val bytesWritten = process.inputStream.use { inputStream ->
                localDbFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            println("[GrpcCapture] ✓ 写入完成，共写入 $bytesWritten bytes")

            // 步骤 5：等待进程结束并检查退出码
            val exitCode = process.waitFor()
            println("[GrpcCapture] adb 进程退出码: $exitCode")

            if (exitCode != 0) {
                val errOutput = process.errorStream.bufferedReader().readText()
                println("[GrpcCapture] ✗ adb 命令错误输出: $errOutput")
            }

            // 步骤 6：校验本地文件
            val fileExists = localDbFile.exists()
            val fileSize = if (fileExists) localDbFile.length() else 0L

            println("[GrpcCapture] 本地文件校验 → 存在: $fileExists | 大小: $fileSize bytes")

            // SQLite 文件头最小为 100 bytes，小于此值说明文件无效
            val success = fileExists && fileSize > 100L
            if (success) {
                println("[GrpcCapture] ✓ 数据库拉取成功！路径: ${localDbFile.absolutePath}")
            } else {
                println("[GrpcCapture] ✗ 数据库拉取失败：文件不存在或内容为空")
                // 清理无效文件
                if (fileExists) localDbFile.delete()
            }
            success
        } catch (e: Exception) {
            println("[GrpcCapture] ✗ refreshDatabase 发生异常: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
