package sophon.desktop.feature.packetcapture.data.source.grpc

/**
 * gRPC body 解码门面（Facade）。
 *
 * 优先使用 [ProtobufSchemaRegistry] 进行 schema-based 解析，
 * 未匹配时自动降级到 [ProtobufSchemalessDecoder]。
 */
internal object GrpcBodyDecoder {

    data class GrpcDecodeResult(
        val compressed: Boolean,
        val messageLength: Int,
        val body: String,
        val isSchemaApplied: Boolean
    )

    fun decode(bytes: ByteArray, grpcPath: String, isRequest: Boolean): GrpcDecodeResult {
        if (bytes.isEmpty()) {
            return GrpcDecodeResult(false, 0, "(空)", false)
        }

        val frame = ProtobufSchemalessDecoder.stripGrpcFrame(bytes)
        val compressed = frame?.compressed ?: false
        val messageLength = frame?.messageLength ?: -1
        // 压缩帧先解压，再交给 schema-based / schemaless 解析
        val protoBody = ProtobufSchemalessDecoder.decompressIfNeeded(
            compressed, frame?.body ?: bytes
        )

        val descriptor = if (isRequest) {
            ProtobufSchemaRegistry.findRequestDescriptor(grpcPath)
        } else {
            ProtobufSchemaRegistry.findResponseDescriptor(grpcPath)
        }

        if (descriptor != null) {
            val (json, _) = ProtobufSchemaRegistry.decodeToJsonWithError(protoBody, descriptor)
            if (json != null) {
                return GrpcDecodeResult(
                    compressed = compressed,
                    messageLength = messageLength,
                    body = buildHeader(compressed, messageLength) + json,
                    isSchemaApplied = true
                )
            }
        }

        return GrpcDecodeResult(
            compressed = compressed,
            messageLength = messageLength,
            body = ProtobufSchemalessDecoder.decode(bytes),
            isSchemaApplied = false
        )
    }

    private fun buildHeader(compressed: Boolean, length: Int): String {
        if (length < 0) return ""
        return "[压缩: ${if (compressed) "是" else "否"} | 长度: $length bytes]\n"
    }
}
