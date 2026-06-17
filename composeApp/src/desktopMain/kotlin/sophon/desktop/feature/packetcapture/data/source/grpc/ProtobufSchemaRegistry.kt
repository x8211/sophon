package sophon.desktop.feature.packetcapture.data.source.grpc

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import sophon.desktop.feature.packetcapture.data.source.grpc.ProtobufSchemaRegistry.descriptors
import sophon.desktop.feature.packetcapture.model.ProtoPath
import java.io.File

/**
 * 基于用户提供的 `.proto` 文件构建 Schema，用于 gRPC 消息的 schema-based 解析。
 *
 * 使用内置 [EmbeddedProtoc] 调用 protoc 编译 .proto 文件，生成标准 [FileDescriptorSet]，
 * 支持完整的 proto2/proto3 语法。
 */
internal object ProtobufSchemaRegistry {

    data class SchemaLoadResult(
        val loadedCount: Int,
        val error: String? = null
    )

    @Volatile
    private var descriptors: Map<String, Descriptor> = emptyMap()

    /**
     * gRPC 方法名 → (请求消息类型全限定名, 响应消息类型全限定名)。
     * 直接从 protoc 编译产出的 [FileDescriptorProto.serviceList] 中读取，
     * 类型名为全限定名（不含前导 `.`），如 `proto.nxt_home.NXTHomePageRequest`。
     */
    @Volatile
    private var serviceMethodMap: Map<String, Pair<String, String>> = emptyMap()

    fun load(paths: List<ProtoPath>): SchemaLoadResult {
        return try {
            val protoFiles = collectProtoFiles(paths)
            if (protoFiles.isEmpty()) {
                descriptors = emptyMap()
                return SchemaLoadResult(0)
            }

            // 调用内置 protoc 编译，获取标准 FileDescriptorSet 二进制
            // proto_path 由 EmbeddedProtoc 从文件实际位置自动推导（最小覆盖集算法）
            val descriptorBytes = EmbeddedProtoc.compileToDescriptorSet(
                protoFiles = protoFiles.map { it.absolutePath }
            ).getOrElse { e ->
                return SchemaLoadResult(0, "protoc 编译失败: ${e.message}")
            }

            val fds = FileDescriptorSet.parseFrom(descriptorBytes)
            val fileProtoMap = fds.fileList.associateBy { it.name }

            serviceMethodMap = buildServiceMethodMap(fds.fileList)

            // 拓扑排序构建 FileDescriptor（依赖在前，被依赖者在后）
            val builtFiles = mutableMapOf<String, FileDescriptor>()
            val newDescriptors = mutableMapOf<String, Descriptor>()
            val visited = mutableSetOf<String>()

            fun buildFile(name: String): FileDescriptor? {
                if (name in builtFiles) return builtFiles[name]
                if (name in visited) return null
                visited.add(name)
                val proto = fileProtoMap[name] ?: return null
                val deps = proto.dependencyList.mapNotNull { buildFile(it) }.toTypedArray()
                val fd = runCatching {
                    FileDescriptor.buildFrom(proto, deps, /*allowUnknownDependencies=*/true)
                }.getOrNull() ?: return null

                builtFiles[name] = fd
                fd.messageTypes.forEach { msg ->
                    newDescriptors[msg.fullName] = msg
                    msg.nestedTypes.forEach { nested -> newDescriptors[nested.fullName] = nested }
                }
                return fd
            }

            fileProtoMap.keys.forEach { buildFile(it) }
            descriptors = newDescriptors

            SchemaLoadResult(newDescriptors.size)
        } catch (e: Exception) {
            descriptors = emptyMap()
            SchemaLoadResult(0, e.message ?: "加载失败")
        }
    }

    fun clear() {
        descriptors = emptyMap()
        serviceMethodMap = emptyMap()
    }

    fun findRequestDescriptor(grpcPath: String): Descriptor? {
        val methodName = extractMethodName(grpcPath) ?: return null
        serviceMethodMap[methodName]?.let { (reqType, _) ->
            findByNameSuffix(reqType)?.let { return it }
        }
        return listOf("${methodName}Request", "${methodName}Req")
            .firstNotNullOfOrNull { findByNameSuffix(it) }
    }

    fun findResponseDescriptor(grpcPath: String): Descriptor? {
        val methodName = extractMethodName(grpcPath) ?: return null
        serviceMethodMap[methodName]?.let { (_, respType) ->
            findByNameSuffix(respType)?.let { return it }
        }
        return listOf("${methodName}Response", "${methodName}Rsp")
            .firstNotNullOfOrNull { findByNameSuffix(it) }
    }

    fun decodeToJson(bytes: ByteArray, descriptor: Descriptor): String? =
        decodeToJsonWithError(bytes, descriptor).first

    /**
     * 解析 protobuf bytes 为 JSON，同时返回失败原因（供诊断日志使用）。
     * @return Pair(json, errorMessage)，json 为 null 时 errorMessage 说明失败原因
     */
    fun decodeToJsonWithError(bytes: ByteArray, descriptor: Descriptor): Pair<String?, String?> {
        return runCatching {
            val msg = DynamicMessage.parseFrom(descriptor, bytes)
            val json = JsonFormat.printer()
                .includingDefaultValueFields()
                .preservingProtoFieldNames()
                .print(msg)
            Pair<String?, String?>(json, null)
        }.getOrElse { e -> Pair(null, e.message) }
    }

    // ─── 内部实现 ────────────────────────────────────────────────────────────

    /**
     * 从 protoc 编译产出的 [FileDescriptorProto] 列表中提取所有 service 方法映射。
     *
     * [FileDescriptorProto.serviceList] → [MethodDescriptorProto.inputType] 格式为全限定名
     * （含前导 `.`，如 `.proto.nxt_home.NXTHomePageRequest`），去掉前导 `.` 后直接与
     * [descriptors] 中以 [Descriptor.fullName] 为 key 的条目精确匹配。
     */
    private fun buildServiceMethodMap(
        fileProtos: List<FileDescriptorProto>
    ): Map<String, Pair<String, String>> {
        val result = mutableMapOf<String, Pair<String, String>>()
        for (fileProto in fileProtos) {
            for (service in fileProto.serviceList) {
                for (method in service.methodList) {
                    val req = method.inputType.removePrefix(".")
                    val resp = method.outputType.removePrefix(".")
                    result[method.name] = req to resp
                }
            }
        }
        return result
    }

    private fun extractMethodName(grpcPath: String): String? {
        val parts = grpcPath.trimStart('/').split('/')
        return parts.lastOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun findByNameSuffix(suffix: String): Descriptor? {
        val current = descriptors
        current[suffix]?.let { return it }
        return current.entries.firstOrNull { entry ->
            entry.key == suffix || entry.key.endsWith(".$suffix")
        }?.value
    }

    private fun collectProtoFiles(paths: List<ProtoPath>): List<File> {
        val result = mutableListOf<File>()
        for (pp in paths) {
            val f = File(pp.path)
            if (!f.exists()) continue
            if (pp.isDirectory) {
                val found = f.walkTopDown()
                    .onEnter { dir ->
                        val name = dir.name
                        name != "build" && name != ".gradle" && name != "generated"
                    }
                    .filter { it.isFile && it.extension == "proto" }
                    .toList()
                result.addAll(found)
            } else {
                if (f.isFile && f.extension == "proto") result.add(f)
            }
        }
        return result.distinctBy { it.canonicalPath }
    }
}
