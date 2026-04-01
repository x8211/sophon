package sophon.desktop.feature.appmonitor.feature.grpc.model

/**
 * gRPC 捕获的数据模型
 *
 * 与 Protodroid 库的 ProtodroidDataEntity Room Entity 一一对应。
 * 各字段在 gRPC 请求生命周期中的作用：
 *
 * @param id              自增主键
 * @param serviceUrl      gRPC 方法全名（如 package.Service/Method），对应 MethodDescriptor.getFullMethodName()
 * @param serviceName     gRPC 服务名（如 package.Service）
 * @param requestHeader   请求元数据 / Headers（序列化后的 Metadata）
 * @param responseHeader  响应元数据 / Trailers
 * @param requestBody     请求体（Protobuf 消息的 JSON 表示）
 * @param responseBody    响应体（Protobuf 消息的 JSON 表示）
 * @param statusCode      gRPC 状态码（整型，如 0=OK, 14=UNAVAILABLE）
 * @param statusLevel     状态级别（0=OK, 1=WARNING, 2=ERROR 等）
 * @param statusName      gRPC Status 枚举名（如 "OK", "UNAVAILABLE"）
 * @param statusDesc      状态描述文本
 * @param statusErrorCause 错误堆栈或原因详情
 * @param createTimestamp  请求发起时间戳（毫秒）
 * @param updateTimestamp  请求完成/更新时间戳（毫秒）
 */
data class GrpcCaptureModel(
    val id: Long,
    val serviceUrl: String,
    val serviceName: String,
    val requestHeader: String,
    val responseHeader: String,
    val requestBody: String,
    val responseBody: String,
    val statusCode: Int,
    val statusLevel: Int,
    val statusName: String,
    val statusDesc: String,
    val statusErrorCause: String,
    val createTimestamp: Long,
    val updateTimestamp: Long
)
