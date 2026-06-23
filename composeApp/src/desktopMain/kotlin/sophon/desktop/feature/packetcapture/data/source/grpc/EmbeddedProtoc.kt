package sophon.desktop.feature.packetcapture.data.source.grpc

import java.io.File
import java.nio.file.Files

/**
 * 内置 protoc 工具封装。
 *
 * protoc 二进制随应用打包在 appResources 中：
 *   - macOS：`macos/tools/protoc-x.y.z-osx-universal/bin/protoc`
 *   - Windows：`windows/tools/protoc-x.y.z-win64/bin/protoc.exe`
 *
 * 打包后 compose.application 会将平台子目录内容平铺到 resources.dir 下，
 * 与 [sophon.desktop.feature.adb.data.source.AdbDataSource] 保持一致的路径约定。
 */
internal object EmbeddedProtoc {

    private val protocPath: String by lazy { resolveProtocPath() }

    /**
     * google/protobuf 标准 well-known types 的 include 目录。
     *
     * 查找顺序：
     * 1. protoc 二进制同级的 `include/` 目录（标准 protoc 发行包结构：bin/../include/）
     * 2. Homebrew 安装路径（macOS 开发机常见）
     * 3. /usr/local/include（Linux / macOS 传统路径）
     */
    private val wellKnownIncludeDir: String? by lazy { resolveWellKnownIncludeDir() }

    private fun resolveProtocPath(): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val binary = if (isWindows) "protoc.exe" else "protoc"

        // 在给定根目录下扫描 protoc-*/bin/protoc（不硬编码版本号）
        fun scanForProtoc(toolsDir: File): File? =
            toolsDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("protoc") }
                ?.map { File(it, "bin/$binary") }
                ?.firstOrNull { it.exists() }

        // 打包后：compose.application 将平台子目录平铺到 resources.dir 下
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            scanForProtoc(File(resourcesDir, "tools"))?.let { return ensureExecutable(it).absolutePath }
        }

        // 开发模式：直接从 appResources 源目录查找
        val platformDir = if (isWindows) "windows" else "macos"
        val candidateRoots = listOf(
            "composeApp/src/desktopMain/appResources/$platformDir/tools",
            "src/desktopMain/appResources/$platformDir/tools",
        )
        candidateRoots.forEach { root ->
            scanForProtoc(File(root))?.let { return ensureExecutable(it).absolutePath }
        }

        return File(candidateRoots.first(), "protoc-*/bin/$binary").absolutePath
    }

    /**
     * 将 protoc 原始错误输出格式化为可读的错误摘要。
     *
     * protoc 失败时通常输出多行：第一行是缺失文件本身（`X.proto: File not found.`），
     * 后续行是因该文件缺失而级联报错的导入方。直接展示原始输出用户难以定位根因，
     * 因此优先提取「缺失依赖」部分并格式化为结构化摘要。
     */
    private fun formatProtocError(exitCode: Int, rawOutput: String): String {
        val lines = rawOutput.lines()

        // 提取所有 "X.proto: File not found." 行中的缺失文件名
        val missingFiles = lines
            .filter { it.trimStart().endsWith(": File not found.") }
            .map { it.trim().removeSuffix(": File not found.") }
            .distinct()

        return if (missingFiles.isNotEmpty()) {
            buildString {
                append("缺少以下 Proto 依赖文件（protoc exit $exitCode）：\n")
                missingFiles.forEach { append("  • $it\n") }
                append("\n")
                append("请将包含上述文件的目录添加到已配置路径，\n")
                append("或将缺失的 .proto 文件放入已配置的目录中。")
            }
        } else {
            "protoc exit $exitCode:\n$rawOutput"
        }
    }

    /** jpackage 打包后内置二进制可能丢失可执行位，与 [AdbRepositoryImpl.autoFindAdbTool] 保持一致。 */
    private fun ensureExecutable(binary: File): File {
        if (binary.exists() && !binary.canExecute()) {
            binary.setExecutable(true)
        }
        return binary
    }

    private fun resolveWellKnownIncludeDir(): String? {
        val protocFile = File(protocPath)
        val candidates = listOf(
            File(protocFile.parentFile.parentFile, "include"),  // bin/../include/ ← 标准结构
            File(protocFile.parentFile, "include"),             // 兼容平铺结构
            File("/opt/homebrew/include"),                      // Homebrew Apple Silicon
            File("/usr/local/include"),                         // Homebrew Intel / Linux
        )
        return candidates.firstOrNull { File(it, "google/protobuf/any.proto").exists() }?.absolutePath
    }

    /**
     * 调用内置 protoc 将 .proto 文件编译为二进制 [com.google.protobuf.DescriptorProtos.FileDescriptorSet]。
     *
     * `--proto_path` 从 [protoFiles] 的实际父目录自动推导：
     * 1. 收集所有 proto 文件的父目录
     * 2. 去掉已有祖先目录的子目录（最小覆盖集），避免子目录改变 import 名解析
     *
     * 示例：`[dev/grpc, dev/grpc/google/api, dev/protos]` → `[dev/grpc, dev/protos]`
     * 这样 `import "google/api/http.proto"` 能被正确解析，同时 `import "nxt_protos_xxx.proto"` 也能找到。
     *
     * @param protoFiles 要编译的 .proto 文件绝对路径列表
     * @return 成功时返回 FileDescriptorSet 二进制内容；失败时返回包含错误信息的 [Result.failure]
     */
    fun compileToDescriptorSet(protoFiles: List<String>): Result<ByteArray> {
        if (protoFiles.isEmpty()) return Result.failure(IllegalArgumentException("protoFiles is empty"))

        // 推导最小 proto_path 集合：保留没有祖先目录的父目录
        val allParentDirs = protoFiles
            .map { File(it).parentFile.canonicalFile }
            .distinctBy { it.canonicalPath }
        val protoPaths = allParentDirs.filter { candidate ->
            allParentDirs.none { other ->
                other != candidate &&
                        candidate.canonicalPath.startsWith(other.canonicalPath + File.separator)
            }
        }.map { it.absolutePath }

        val outFile = Files.createTempFile("sophon-proto", ".pb").toFile()
        return try {
            val cmd = buildList {
                add(protocPath)
                wellKnownIncludeDir?.let { add("--proto_path=$it") }
                protoPaths.forEach { add("--proto_path=$it") }
                add("--descriptor_set_out=${outFile.absolutePath}")
                add("--include_imports")
                addAll(protoFiles)
            }

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.readBytes().toString(Charsets.UTF_8)
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                Result.failure(RuntimeException(formatProtocError(exitCode, output)))
            } else {
                Result.success(outFile.readBytes())
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            outFile.delete()
        }
    }
}
