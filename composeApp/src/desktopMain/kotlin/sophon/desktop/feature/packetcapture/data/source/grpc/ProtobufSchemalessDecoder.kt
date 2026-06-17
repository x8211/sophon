package sophon.desktop.feature.packetcapture.data.source.grpc

/**
 * 无 Schema 的 gRPC/Protobuf 解码器，不依赖任何外部库。
 *
 * gRPC DATA frame 格式：
 *   [1 byte 压缩标志][4 bytes 消息长度 big-endian][N bytes protobuf 二进制]
 *
 * Protobuf wire format（无 schema 可解析骨架）：
 *   每个字段 = varint(field_number << 3 | wire_type) + 值
 *   wire_type: 0=varint, 1=64-bit fixed, 2=length-delimited, 5=32-bit fixed
 */
internal object ProtobufSchemalessDecoder {

    data class GrpcFrame(
        val compressed: Boolean,
        val messageLength: Int,
        val body: ByteArray
    )

    /**
     * 解析 gRPC 5 字节帧头，返回 [GrpcFrame]。
     * 支持多帧流（gRPC server-streaming），返回首个帧。
     * @return null 表示数据不足或格式错误
     */
    fun stripGrpcFrame(bytes: ByteArray): GrpcFrame? {
        if (bytes.size < 5) return null
        val compressed = bytes[0] != 0.toByte()
        val length = ((bytes[1].toInt() and 0xFF) shl 24) or
                ((bytes[2].toInt() and 0xFF) shl 16) or
                ((bytes[3].toInt() and 0xFF) shl 8) or
                (bytes[4].toInt() and 0xFF)
        if (length < 0 || bytes.size < 5 + length) return null
        val body = bytes.copyOfRange(5, 5 + length)
        return GrpcFrame(compressed, length, body)
    }

    /**
     * 解码完整 gRPC body（含帧头）。
     * 返回多行缩进文本，示例：
     * ```
     * [压缩: false | 长度: 42 bytes]
     * 1: "hello world"
     * 2: 12345
     * 3 {
     *   1: "nested"
     * }
     * ```
     */
    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "(空)"
        val frame = stripGrpcFrame(bytes) ?: return decodeRawBytes(bytes, 0)
        val sb = StringBuilder()
        sb.appendLine("[压缩: ${if (frame.compressed) "是" else "否"} | 长度: ${frame.messageLength} bytes]")
        sb.append(decodeMessage(frame.body, 0))
        return sb.toString().trimEnd()
    }

    // ─── 内部实现 ───────────────────────────────────────────────────────────

    private fun decodeMessage(bytes: ByteArray, depth: Int): String {
        if (bytes.isEmpty()) return indent(depth) + "(空消息)\n"
        val sb = StringBuilder()
        var offset = 0
        while (offset < bytes.size) {
            val tagResult = readVarint(bytes, offset)
            if (tagResult == null || tagResult.value == 0L) break
            offset = tagResult.nextOffset
            val tag = tagResult.value
            val fieldNumber = (tag ushr 3).toInt()
            val wireType = (tag and 0x7L).toInt()

            when (wireType) {
                0 -> { // Varint
                    val v = readVarint(bytes, offset) ?: break
                    offset = v.nextOffset
                    sb.append(indent(depth)).append("$fieldNumber: ${v.value}\n")
                }
                1 -> { // 64-bit fixed
                    if (offset + 8 > bytes.size) break
                    val lo = readFixed32(bytes, offset)
                    val hi = readFixed32(bytes, offset + 4)
                    val value = (hi.toLong() shl 32) or (lo.toLong() and 0xFFFFFFFFL)
                    offset += 8
                    sb.append(indent(depth)).append("$fieldNumber: $value (fixed64)\n")
                }
                2 -> { // Length-delimited
                    val lenResult = readVarint(bytes, offset) ?: break
                    offset = lenResult.nextOffset
                    val len = lenResult.value.toInt()
                    if (len < 0 || offset + len > bytes.size) break
                    val data = bytes.copyOfRange(offset, offset + len)
                    offset += len
                    sb.append(decodeDelimited(fieldNumber, data, depth))
                }
                5 -> { // 32-bit fixed
                    if (offset + 4 > bytes.size) break
                    val value = readFixed32(bytes, offset)
                    offset += 4
                    sb.append(indent(depth)).append("$fieldNumber: $value (fixed32)\n")
                }
                else -> break // 未知 wire type，停止解析
            }
        }
        return if (sb.isEmpty()) indent(depth) + "(无字段)\n" else sb.toString()
    }

    /**
     * 对 length-delimited 字段尝试三种解析：
     * 1. UTF-8 字符串（含可打印字符判断）
     * 2. 递归 protobuf 嵌套消息
     * 3. 原始字节摘要
     */
    private fun decodeDelimited(fieldNumber: Int, data: ByteArray, depth: Int): String {
        if (data.isEmpty()) return indent(depth) + "$fieldNumber: \"\"\n"

        // 尝试 UTF-8 字符串
        val asStr = runCatching { data.toString(Charsets.UTF_8) }.getOrNull()
        if (asStr != null && isPrintable(asStr)) {
            return indent(depth) + "$fieldNumber: \"$asStr\"\n"
        }

        // 尝试递归嵌套消息
        val nested = runCatching { decodeMessage(data, depth + 1) }.getOrNull()
        if (nested != null && !nested.contains("(无字段)") && !nested.contains("(空消息)")) {
            val sb = StringBuilder()
            sb.append(indent(depth)).append("$fieldNumber {\n")
            sb.append(nested)
            sb.append(indent(depth)).append("}\n")
            return sb.toString()
        }

        // 原始字节
        return decodeRawBytes(data, depth, fieldNumber)
    }

    private fun decodeRawBytes(data: ByteArray, depth: Int, fieldNumber: Int? = null): String {
        val hex = data.take(16).joinToString(" ") { "%02x".format(it) }
        val suffix = if (data.size > 16) "..." else ""
        val prefix = if (fieldNumber != null) "$fieldNumber: " else ""
        return indent(depth) + "${prefix}<bytes: ${data.size}> [$hex$suffix]\n"
    }

    private fun isPrintable(s: String): Boolean {
        if (s.length > 1024) return false
        return s.all { it.code in 0x09..0x0D || it.code in 0x20..0x7E || it.code > 0x7F }
    }

    private fun indent(depth: Int) = "  ".repeat(depth)

    private data class VarintResult(val value: Long, val nextOffset: Int)

    private fun readVarint(bytes: ByteArray, startOffset: Int): VarintResult? {
        var value = 0L
        var shift = 0
        var offset = startOffset
        while (offset < bytes.size && shift < 64) {
            val b = bytes[offset++].toInt() and 0xFF
            value = value or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return VarintResult(value, offset)
            shift += 7
        }
        return null
    }

    private fun readFixed32(bytes: ByteArray, offset: Int): Long {
        return ((bytes[offset].toInt() and 0xFF).toLong()) or
                ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF).toLong() shl 24)
    }
}
