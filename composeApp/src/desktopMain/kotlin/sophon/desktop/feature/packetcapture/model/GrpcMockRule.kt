package sophon.desktop.feature.packetcapture.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 单条 gRPC MapLocal Mock 规则。
 *
 * 匹配条件：[host]（"*" 表示通配所有 host）+ [path]（格式 `/pkg.Service/Method`）。
 * 响应：[responseJson] 经 [sophon.desktop.feature.packetcapture.data.source.grpc.ProtobufSchemaRegistry]
 * 编码为 protobuf，再包 5 字节 gRPC 帧头写回客户端。
 */
@Serializable
data class GrpcMockRule(
    val id: String = UUID.randomUUID().toString(),
    val host: String,
    val path: String,
    val responseJson: String = "{}",
    val grpcStatus: Int = 0,
    val enabled: Boolean = true,
)
