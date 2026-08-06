# PacketCapture 功能模块架构文档

## 1. 模块概览

`packetcapture` 是一个基于 **Netty MITM 代理**实现的 HTTP/HTTPS 流量抓包功能模块。它在本机启动一个本地代理服务器（默认端口 8888），拦截流经该代理的 HTTP 明文请求和 HTTPS/HTTP2/gRPC 加密请求，并实时呈现在 UI 界面中。

**核心能力**：
- HTTP/1.1 明文流量透明转发与捕获
- HTTPS 流量中间人（MITM）解密与捕获（需设备安装自签 CA 证书）
- HTTP/2 及 gRPC over h2 流量捕获（ALPN 协商，按流配对）
- 动态生成按域名定制的叶子证书（BouncyCastle）
- gRPC Protobuf 解码（支持 Schema-based 与无 Schema 启发式两种模式）
- **gRPC MapLocal Mock**：按 host + path 拦截 gRPC 请求，短路返回自定义响应（无需访问真实后端）
- 运行时可调的网络限速（`GlobalTrafficShapingHandler`）
- 实时流量列表展示、关键词过滤、按 Host 分组树形视图、请求详情查看
- 通过 `adb push` 将 CA 证书推送至 Android 设备并提供安装引导

---

## 2. 目录结构

```text
packetcapture/
├── ARCHITECTURE.md
├── model/
│   ├── CapturedPacket.kt              # 捕获记录（sealed interface：Http / Grpc）
│   ├── CaptureState.kt                # UI 单一状态源（含过滤、分组、Schema、限速、Mock 派生属性）
│   ├── ThrottleConfig.kt              # 网络限速配置（预设档位 + 自定义 Kbps）
│   ├── ProtoPath.kt                   # 用户配置的 .proto 路径（持久化）
│   └── GrpcMockRule.kt                # gRPC Mock 规则（持久化）
├── data/
│   ├── repository/
│   │   ├── PacketCaptureRepository.kt     # 数据层对外接口
│   │   └── PacketCaptureRepositoryImpl.kt # 数据层实现
│   └── source/
│       ├── MitmProxyDataSource.kt         # Netty 服务器入口（生命周期 + 限速管理）
│       ├── mitm/                          # Netty MITM pipeline handlers
│       │   ├── ProxyFrontendHandler.kt    # HTTP/HTTPS CONNECT 路由与前端处理
│       │   ├── HttpsMitmHandler.kt        # HTTP/1.1 MITM：请求侧拦截 + PendingRequest 入队
│       │   ├── BackendResponseHandler.kt  # HTTP/1.1 MITM：后端响应侧流式接收与回写
│       │   ├── Http2MitmHandler.kt        # HTTP/2 MITM：前后端 codec 装配 + 流配对
│       │   ├── BackendChannelManager.kt   # HTTP/2 后端连接生命周期（GOAWAY 自动重连）
│       │   └── NettyExtensions.kt         # Netty Future → Kotlin suspend 桥接工具
│       ├── cert/
│       │   └── CertificateAuthority.kt    # CA 证书管理与按 host 动态签发叶子证书
│       ├── protocol/
│       │   ├── ProtocolDetector.kt        # 两阶段协议检测（PlainHttp/CONNECT + ALPN）
│       │   ├── InboundRequest.kt          # 第一阶段结果模型（sealed interface）
│       │   ├── MitmProtocol.kt            # 第二阶段结果模型（Http1 / Http2）
│       │   ├── Http1MitmSession.kt        # HTTP/1.1 MITM 会话装配
│       │   └── Http2MitmSession.kt        # HTTP/2 MITM 会话装配
│       └── grpc/
│           ├── GrpcDetector.kt            # gRPC 请求识别（Content-Type 判断）
│           ├── GrpcBodyDecoder.kt         # gRPC body 解码门面（schema-based → schemaless）
│           ├── GrpcMockRegistry.kt        # Mock 规则注册表（@Volatile 无锁，供 Netty EventLoop 查询）
│           ├── ProtobufSchemaRegistry.kt  # Schema 构建与 gRPC 方法→消息类型映射
│           ├── ProtobufSchemalessDecoder.kt # 无 Schema 启发式 Protobuf 帧解析
│           └── EmbeddedProtoc.kt          # 内置 protoc 封装（编译 .proto → FileDescriptorSet）
└── ui/
    ├── PacketCaptureScreen.kt             # 主屏幕 Composable（布局骨架与对话框）
    ├── PacketCaptureViewModel.kt          # 状态容器（StateFlow + viewModelScope）
    └── components/
        ├── CaptureToolbar.kt              # 工具栏（开始/停止/清空/过滤/CA/限速/Schema/Mock）
        ├── HostTreePanel.kt               # 按 host 分组的树形列表（MOCK 标记）
        ├── PacketDetailPanel.kt           # 五标签详情面板（含 gRPC body 解码展示与树形节点视图）
        ├── ProtoSchemaDialog.kt           # Proto 路径管理对话框
        ├── ThrottleDialog.kt              # 限速配置对话框
        ├── GrpcMockDialog.kt              # gRPC Mock 规则管理对话框（树形内联编辑）
        └── JsonTreeView.kt                # JSON 折叠树视图组件（只读 + 内联编辑两套实现）
```

**外部集成**：`AppScreen.kt` 中注册路由 `AppScreen.PacketCapture`，通过 `PacketCaptureScreen()` 渲染本模块。

---

## 3. 分层架构

本模块严格遵循 [Clean Architecture 三层规范](../../../../../../../../../../docs/desktop_rules.md)，依赖方向自上而下，数据流向自下而上：

```
┌─────────────────────────────────────────────────────────────┐
│                        界面层 (UI)                           │
│  PacketCaptureScreen / ViewModel                            │
│  CaptureToolbar / HostTreePanel / PacketListPanel /         │
│  PacketDetailPanel / ThrottleDialog / ProtoSchemaDialog /   │
│  GrpcMockDialog / JsonTreeView                              │
├─────────────────────────────────────────────────────────────┤
│                       数据层 (Data)                          │
│  PacketCaptureRepository（接口边界）                         │
│  PacketCaptureRepositoryImpl                                │
│  ├── MitmProxyDataSource（Netty 服务器 + 限速）              │
│  │   └── mitm/                                              │
│  │       ├── ProxyFrontendHandler                           │
│  │       │   ├── protocol/Http1MitmSession                  │
│  │       │   │   ├── HttpsMitmHandler                       │
│  │       │   │   └── BackendResponseHandler                 │
│  │       │   └── protocol/Http2MitmSession                  │
│  │       │       ├── Http2MitmHandler                       │
│  │       │       └── BackendChannelManager                  │
│  │       └── NettyExtensions                                │
│  └── cert/CertificateAuthority                              │
├─────────────────────────────────────────────────────────────┤
│                      模型层 (Model)                          │
│  CapturedPacket / CaptureState / ThrottleConfig / ProtoPath │
│  （被所有层共享，不可变）                                    │
└─────────────────────────────────────────────────────────────┘
```

> 本模块无 Domain 层（无需 UseCase），`Repository` 直接作为 `ViewModel` 的下层依赖。

---

## 4. 核心组件详解

### 4.1 模型层 (`model/`)

| 类 | 职责 |
|---|---|
| `CapturedPacket` | 单条抓包记录；`sealed interface` 含 `Http`（明文/TLS）与 `Grpc` 两个子类型；不可变 `data class`，含 `url`、`isComplete` 等计算属性；`Grpc` 子类型含 `isMocked` 标记（由 Mock 短路拦截产生） |
| `CaptureState` | UI 单一状态源，聚合 `status`、`port`、`packets`、`selectedPacketId`、`filterText`、`protoPath`、`throttleConfig`、`mockRules`、`showMockDialog`、`editingMockRule`、`mockEncodeErrors` 等字段；`filteredPackets`、`groupedByHost` 等为派生属性 |
| `ThrottleConfig` | 网络限速配置；`ThrottlePreset` 枚举对应 5G/4G/3G/2G/自定义档位；`effectiveDownloadBps`/`effectiveUploadBps` 返回对应的 Netty 字节速率 |
| `ProtoPath` | 用户配置的 `.proto` 文件路径，`@Serializable data class`，持久化至 `{CACHE_HOME}/proto_paths.json` |
| `GrpcMockRule` | 单条 gRPC Mock 规则，`@Serializable data class`；`host`（支持 `*` 通配）+ `path` 为匹配条件；`responseJson` 为响应报文 JSON 文本；持久化至 `{CACHE_HOME}/mock_rules.json` |

### 4.2 数据层 (`data/`)

#### Repository

- **`PacketCaptureRepository`**（接口）：声明 `startCapture(port): Flow<CapturedPacket>`、`stopCapture()`、`installCaToDevice()`、`getDeviceProxy()`、`getCaCertPath()`、`updateThrottle(config)`、`updateMockRules(rules, encodedBodies)` 七个合约。`ViewModel` 仅依赖接口，便于测试替换。
- **`PacketCaptureRepositoryImpl`**（实现）：
  - 通过 `callbackFlow` 桥接 Netty 回调与 Kotlin 协程。
  - 持有 `MitmProxyDataSource` 引用，负责生命周期管理（`start` / `stop`）与限速转发。
  - 通过注入的 `ProxyRepository` 读取当前设备代理配置。
  - 通过 `adb push` + `CertificateAuthority.getCaCertFile()` 完成 CA 证书推送。

#### DataSource (`source/`)

| 类 | 职责 |
|---|---|
| `MitmProxyDataSource` | Netty `ServerBootstrap` 封装；管理 `bossGroup`/`workerGroup`/`scope` 生命周期；以 `AtomicLong` 生成全局单调递增 packet id；持有 `GlobalTrafficShapingHandler` 实例并通过 `updateThrottle()` 动态调整限速 |

#### MITM Pipeline (`source/mitm/`)

| 类/文件 | 职责 |
|---|---|
| `ProxyFrontendHandler` | 前端入站处理器；区分 `PlainHttp`/`ConnectTunnel` 并路由；CONNECT 路径在协程中顺序执行双向 TLS 握手、ALPN 检测后委托 Session 类装配 MITM 管道 |
| `HttpsMitmHandler` | HTTP/1.1 请求侧；将请求元数据压入 `ArrayDeque<PendingRequest>` 后转发后端；`isGrpc` 标记在入队时由 `GrpcDetector` 赋值 |
| `BackendResponseHandler` | HTTP/1.1 响应侧；流式接收（不依赖 `HttpObjectAggregator`）；实时转发前端；最多缓存 1 MB 响应体用于 UI 展示 |
| `Http2MitmHandler` | 提供 `addHttp2BackendCodec()` 和 `addHttp2FrontendPipeline()` 两个包级函数；按流配对请求/响应；支持 gRPC server-streaming 实时转发 |
| `BackendChannelManager` | 管理单条 HTTP/2 后端连接；GOAWAY 后自动重连；并发重连保护（`pendingActions` 队列），所有方法须在同一 EventLoop 线程调用 |
| `NettyExtensions` | Netty `ChannelFuture`/`Future<Channel>` → Kotlin `suspend` 桥接：`awaitChannel()`、`awaitWrite(onSuccess)`、`awaitHandshake(onSuccess)`；`onSuccess` 在 EventLoop 线程同步执行，消除 pipeline 操作的竞争窗口 |

#### CA 证书管理 (`source/cert/`)

| 类 | 职责 |
|---|---|
| `CertificateAuthority` | **单例**。CA 证书持久化到 `{CACHE_HOME}/ca/`（首次启动自动生成，10 年有效期）；按 host 动态签发叶子证书（1 年有效期）；`getSslContextFor(host, supportH2)` 按 `host|h1`/`host|h2` 缓存至 `ConcurrentHashMap<String, SslContext>`，`supportH2` 参数控制前端 ALPN 广播范围，须与后端协商结果一致以避免 FRAME_SIZE_ERROR |

#### 协议检测 (`source/protocol/`)

| 类 | 职责 |
|---|---|
| `ProtocolDetector` | 两阶段纯函数检测：`detect()` 从 HTTP 请求判断 `PlainHttp`/`ConnectTunnel`；`detectMitm()` 从双端 ALPN 判断 `Http1`/`Http2`；无副作用，可独立单测 |
| `InboundRequest` | 第一阶段结果 `sealed interface`（`PlainHttp` 含 host/port/path/isGrpc，`ConnectTunnel` 含 host/port） |
| `MitmProtocol` | 第二阶段结果 `sealed interface`（`Http1`/`Http2` data object） |
| `Http1MitmSession` | HTTP/1.1 MITM 会话封装；`install()` 内聚创建 `ArrayDeque<PendingRequest>` 并一次性装配前端 `HttpsMitmHandler` + 后端 `BackendResponseHandler` |
| `Http2MitmSession` | HTTP/2 MITM 会话封装；`install()` 调用 `addHttp2FrontendPipeline()` 装配前端管道 |

#### gRPC 解码 (`source/grpc/`)

| 类 | 职责 |
|---|---|
| `GrpcDetector` | 根据 `Content-Type: application/grpc*` 判断是否为 gRPC 请求；纯函数，不修改流量 |
| `GrpcBodyDecoder` | 解码门面；优先尝试 `ProtobufSchemaRegistry` schema-based 解析，降级至 `ProtobufSchemalessDecoder` |
| `GrpcMockRegistry` | **单例**。Mock 规则注册表；使用 `@Volatile` + 不可变列表替换（无锁读写）保证线程安全；`ViewModel`（IO 线程）调用 `update()` 写入，Netty EventLoop 线程调用 `findMatch()` 只读查询 |
| `ProtobufSchemaRegistry` | 基于用户 `.proto` 文件构建 Schema；调用 `EmbeddedProtoc` 编译；维护 gRPC 方法→消息类型映射；使用 `DynamicMessage` + `JsonFormat` 解码为 JSON；`encodeFromJson()` 将 JSON 反向编码为 Protobuf 字节（Mock 响应体使用） |
| `ProtobufSchemalessDecoder` | 无 Schema 启发式解析；剥离 gRPC 帧头（5 字节）后对 Protobuf wire format 进行字段级拆解 |
| `EmbeddedProtoc` | 内置 protoc 二进制封装；从 appResources 解析路径，通过 `ProcessBuilder` 编译 `.proto` 为 `FileDescriptorSet` |

### 4.3 界面层 (`ui/`)

| 类 | 职责                                                                                                                                                                                                                                                                 |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PacketCaptureViewModel` | 持有 `MutableStateFlow<CaptureState>`；`startCapture()` 在 `Dispatchers.IO` 上收集 `Flow<CapturedPacket>`；管理 Proto Schema 持久化（读写 `proto_paths.json`）；管理 Mock 规则增删改启禁用与持久化（读写 `mock_rules.json`）；启动时顺序执行 Schema 加载 → Mock 规则恢复 → 静默重编码，确保规则开机即生效；`onCleared()` 确保代理随生命周期停止 |
| `PacketCaptureScreen` | 顶层骨架 Composable：`CaptureToolbar` + `Row(HostTreePanel + PacketListPanel + PacketDetailPanel)`；托管 `ThrottleDialog`、`ProtoSchemaDialog`、`GrpcMockDialog`、CA 安装引导对话框                                                                                                  |
| `CaptureToolbar` | 状态驱动的「开始」/「停止」按钮；搜索框；CA 安装、限速、Proto Schema、**Mock 管理**入口                                                                                                                                                                                                           |
| `HostTreePanel` | 按 host 分组的树形列表，支持展开/收起；**MOCK 标记**高亮展示被拦截包                                                                                                                                                                                                                         |
| `PacketListPanel` | `LazyColumn` 虚拟滚动；新数据到来时自动 `animateScrollToItem` 至末尾；HTTP Method 与状态码以语义色彩区分                                                                                                                                                                                       |
| `PacketDetailPanel` | `ScrollableTabRow` 五标签页（概览/请求头/请求体/响应头/响应体）；gRPC body 有 Schema 时渲染 `JsonTreeView`（折叠树节点），无 Schema 时纯文本降级；Body 自动检测 `Content-Type` 进行 JSON 美化                                                                                                                       |
| `ThrottleDialog` | 限速配置对话框；展示预设档位（5G/4G/3G/2G）与自定义输入                                                                                                                                                                                                                                  |
| `ProtoSchemaDialog` | `.proto` 路径管理对话框；增删路径后触发 `ProtobufSchemaRegistry` 重新编译                                                                                                                                                                                                             |
| `GrpcMockDialog` | gRPC Mock 规则管理对话框；规则列表（启用开关 + 编辑 + 删除）；响应 JSON 以 `JsonTreeEditor` 展示（可折叠树节点内联编辑叶子值，Enter 提交/Esc 取消）；对话框高度自适应窗口高度（85%）                                                                                                                                              |
| `JsonTreeView` | 共享 JSON 树视图组件；提供两套接口：`JsonTreeView(JsonElement?)` 只读树（`PacketDetailPanel` 使用）、`JsonTreeEditor(jsonText, onJsonChange)` 内联编辑树（`GrpcMockDialog` 使用）                                                                                                                  |

---

## 5. HTTPS/HTTP2 MITM 核心流程

### 5.1 CONNECT 握手与管道建立（协程化）

```
客户端发送 CONNECT {host}:{port} HTTP/1.1
         │
         ▼
ProxyFrontendHandler.handleConnect()  [CoroutineScope.launch]
         │
         ├─ 1. TCP 连接后端（Bootstrap.connect().awaitChannel()）
         │
         ├─ 2. 后端 TLS 握手（backendSsl.handshakeFuture().awaitHandshake {
         │      onSuccess 内同步执行：读取 ALPN，若为 h2 立即 addHttp2BackendCodec()
         │   }）
         │
         ├─ 3. 向客户端回复 200 Connection Established（writeAndFlush().awaitWrite {
         │      onSuccess 内同步执行：清除前端 HTTP codec，注入前端 SslContext
         │   }）—— 原子操作，消除管道空窗期
         │
         ├─ 4. 前端 TLS 握手（frontendSsl.handshakeFuture().awaitHandshake {
         │      onSuccess 内同步执行：ProtocolDetector.detectMitm() 判断协议
         │      → Http1：Http1MitmSession.install()
         │      → Http2：BackendChannelManager.donateChannel() + Http2MitmSession.install()
         │      → 将 trafficShapingHandler 插入 pipeline（限速生效）
         │   }）
         │
         └─ 管道就绪，后续流量由对应 Session 管理
```

> **时序关键**：`addHttp2BackendCodec()` 必须在后端握手完成后**立即**在 EventLoop 线程执行，
> 否则服务端初始 SETTINGS 帧在 codec 就位前到达，导致 `First received frame was not SETTINGS`。

### 5.2 HTTP/1.1 MITM 管道结构

```
[客户端] ←── frontendChannel ──→ [ProxyServer]
  Pipeline:
    frontendSsl            ← CertificateAuthority 动态叶子证书（含 ALPN h1/h2）
    trafficShaping         ← GlobalTrafficShapingHandler（限速，握手后插入）
    httpServerCodecMitm
    httpAggregatorMitm
    HttpsMitmHandler       ← 记录请求到 ArrayDeque<PendingRequest>，转发至后端

[ProxyServer] ←── backendChannel ──→ [真实服务器]
  Pipeline:
    backendSsl             ← InsecureTrustManagerFactory
    httpClientCodec        ← 流式（无 HttpObjectAggregator，支持任意大小响应）
    BackendResponseHandler ← 从 ArrayDeque 弹出请求，组装 CapturedPacket 并回调

共享状态：ArrayDeque<PendingRequest>（FIFO，HTTPS 下一条连接一般只有一个在途请求）
```

### 5.3 HTTP/2 / gRPC MITM 管道结构

```
[客户端] ←── frontendChannel ──→ [ProxyServer]
  Pipeline:
    frontendSsl
    trafficShaping
    Http2FrameCodec (server)   ← 握手完成后由 addHttp2FrontendPipeline() 装配
    Http2MultiplexHandler
      └─ [每条流] Http2FrontendStreamHandler
           │  缓冲请求帧至 END_STREAM，通过 BackendChannelManager 获取后端连接
           └─ [后端子流] Http2BackendStreamHandler
                实时转发响应帧，END_STREAM 时触发 CapturedPacket 回调

[ProxyServer] ←── backendChannel ──→ [真实服务器]
  Pipeline:
    backendSsl
    Http2FrameCodec (client)   ← 后端握手完成后由 addHttp2BackendCodec() 提前装配
    Http2MultiplexHandler (server push 静默丢弃)

BackendChannelManager：GOAWAY 后自动重连，pendingActions 队列保证并发安全
```

### 5.4 运行时数据流

```
用户点击「开始」
  → ViewModel.startCapture()
  → Repository.startCapture(port)  [callbackFlow]
  → MitmProxyDataSource.start(port)  [Netty bind]
      │
      ├─ HTTP 明文请求到达
      │    → ProxyFrontendHandler.handlePlainHttp()
      │    → 连接后端、透传、捕获响应
      │    → CapturedPacket.Http(scheme="http") → callbackFlow.trySend()
      │
      └─ HTTPS/HTTP2/gRPC CONNECT 请求到达
           → ProxyFrontendHandler.handleConnect()  [coroutine]
           → [MITM 管道建立，见 5.1]
           → Http1：HttpsMitmHandler → BackendResponseHandler
           → Http2：Http2FrontendStreamHandler → Http2BackendStreamHandler
           → CapturedPacket.Http/Grpc(scheme="https") → callbackFlow.trySend()
                │
                ▼
  ViewModel._uiState.update { packets + packet }
  UI 列表实时追加，HostTreePanel / PacketListPanel 同步刷新
```

---

## 6. gRPC Protobuf 解码

UI 层 `PacketDetailPanel` 在展示 gRPC 包体时调用 `GrpcBodyDecoder.decode()`：

```
GrpcBodyDecoder.decode(grpcBody, grpcPath)
  │
  ├─ ProtobufSchemaRegistry.decode()  （有 Schema 时）
  │    ← 用户通过 ProtoSchemaDialog 添加 .proto 路径
  │    ← EmbeddedProtoc 编译 .proto → FileDescriptorSet
  │    ← DynamicMessage + JsonFormat 解码为 JSON
  │
  └─ ProtobufSchemalessDecoder.decode()  （无 Schema 降级）
       ← 剥离 5 字节 gRPC 帧头
       ← Protobuf wire format 启发式字段级拆解
```

Proto Schema 持久化：`ProtoPath` 列表存储于 `{CACHE_HOME}/proto_paths.json`，由 `ViewModel` 在启动时加载并传入 `ProtobufSchemaRegistry`。

---

## 7. gRPC MapLocal Mock

### 7.1 概述

Mock 功能实现「MapLocal」模式：对匹配 host + path 的 gRPC 请求，在代理层直接短路返回用户预设的响应体，不访问真实后端。

### 7.2 数据流

```
用户在 GrpcMockDialog 编辑规则
  → ViewModel.saveRule(rule)
       ├── 调用 ProtobufSchemaRegistry.encodeFromJson(path, responseJson)
       │     将 JSON 文本编码为 Protobuf 二进制（加 5 字节 gRPC 帧头）
       ├── 将 encodedBody 存入 Map<ruleId, ByteArray>
       ├── 持久化规则列表 → {CACHE_HOME}/mock_rules.json
       └── 调用 Repository.updateMockRules(rules, encodedBodies)
                → GrpcMockRegistry.update()   @Volatile 原子替换

Netty EventLoop 线程处理 HTTP/2 请求
  → Http2FrontendStreamHandler.onRequestComplete()
       ├── isGrpc == true
       │     → GrpcMockRegistry.findMatch(host, path)
       │          host == "*" 通配，或精确匹配
       │
       ├── 命中（MockResult(encodedBody, grpcStatus)）
       │     → writeMockResponse()
       │          ① 向客户端写入预编码 DATA 帧（encodedBody）
       │          ② 写入 HEADERS 帧（grpc-status: N, grpc-message）
       │          ③ END_STREAM，不建立后端连接
       │          ④ 上报 CapturedPacket.Grpc(isMocked=true)
       │
       └── 未命中 → 正常透传至真实后端
```

### 7.3 线程安全设计

`GrpcMockRegistry` 采用 **`@Volatile` + 不可变列表原子替换** 策略：

- **写**：`ViewModel`（`Dispatchers.IO`）调用 `update()`，将过滤并预编码的规则列表整体替换。
- **读**：Netty EventLoop 线程调用 `findMatch()`，读取 `@Volatile` 字段的快照，零锁开销。
- 无需 `synchronized` 或 `ConcurrentHashMap`，满足高吞吐场景下的快路径查询。

### 7.4 JSON 编辑器设计（JsonTreeEditor）

`JsonTreeEditor` 基于 `kotlinx.serialization.json` 的 `JsonElement` API，实现**不可变路径更新**：

```
PathSeg = Key(name: String) | Idx(index: Int)   // 路径节点类型
InlineEditState(path, textValue, hasError)        // 当前编辑状态

点击叶子节点 (JsonPrimitive)
  → onEditStart(path, initialText)
       editState = InlineEditState(path, textValue = TextFieldValue(text, selection=all))

用户修改文字（BasicTextField）
  → onEditTextChange(newValue)
       editState = editState.copy(textValue = newValue, hasError = false)

Enter / 失焦
  → onEditCommit(path, rawText)
       parseRawValue(rawText)   null / true / false / 数字 / "字符串"
       ├── 成功 → root.updateAtPath(path, newPrimitive)  // 递归创建新 JsonObject/JsonArray
       │          prettyPrint → onJsonChange(newJson)
       │          editState = null
       └── 失败 → editState = editState.copy(hasError = true)  // 边框变红

Escape / 失焦（无法解析）
  → onEditCancel()
       editState = null  // 恢复原值
```

**焦点管理**：`BasicTextField` 通过 `FocusRequester` 在进入编辑态时自动获焦；`onFocusChanged` 内以 `wasFocused` 标记区分「首次渲染触发 isFocused=false」和「真实失焦事件」，防止编辑态立即闪回（已知 Compose 焦点回调时序问题）。

---

## 8. 网络限速

限速通过 Netty `GlobalTrafficShapingHandler` 实现，**在 TLS 握手完成后才插入 pipeline**，作用于解密后的明文流：

- 初始化时由 `MitmProxyDataSource.start()` 创建，绑定至 `workerGroup`（500ms 统计窗口，15s 最大延迟）。
- `ProxyFrontendHandler` 在 MITM 管道安装完成后，将其插入 `frontendSsl` 之后，确保不干扰握手阶段。
- `updateThrottle(config)` 在运行时直接调用 `GlobalTrafficShapingHandler.configure()`，无需重启代理。
- 代理未运行时的配置变更会在下次 `start()` 时应用。

---

## 9. 外部依赖

| 依赖 | 用途 |
|---|---|
| **Netty** (`io.netty.*`) | HTTP/1.1 + HTTP/2 代理服务器、事件循环、Pipeline、SSL/ALPN 处理、限速 |
| **BouncyCastle** (`org.bouncycastle.*`) | CA 证书与叶子证书动态生成、PEM 格式读写 |
| **Protobuf** (`com.google.protobuf`) | `DynamicMessage`、`Descriptor`、`JsonFormat`（gRPC schema-based 解码） |
| **kotlinx.serialization.json** | 响应体 JSON 美化展示；`ProtoPath` 持久化 |
| `sophon.desktop.core.Shell.simpleShell` | `adb push` 推送 CA 证书到设备 |
| `sophon.desktop.feature.proxy.data.repository.ProxyRepository` | 读取当前设备代理配置 |
| `sophon.desktop.core.CACHE_HOME` | CA 证书（`{CACHE_HOME}/ca/`）与 Proto 路径（`{CACHE_HOME}/proto_paths.json`）持久化目录 |

---

## 10. 设计约束与注意事项

| 约束 | 说明 |
|---|---|
| 最大请求体 10 MB | 前端 `HttpObjectAggregator(10 MB)`；后端响应侧流式接收，无聚合限制，UI 展示最多缓存 1 MB |
| CA 证书有效期 10 年 | 首次启动生成，持久化复用；如需更新需手动删除 `{CACHE_HOME}/ca/` 目录 |
| 叶子证书有效期 1 年 | 按 `host|h1`/`host|h2` 缓存在内存，重启后重新生成 |
| HTTP/2 ALPN 一致性 | 前端 `SslContext.supportH2` 须与后端 ALPN 结果一致，否则触发 FRAME_SIZE_ERROR |
| gRPC 流式支持限制 | 当前实现缓冲请求帧至 END_STREAM 再转发，客户端流式/双向流式调用在 END_STREAM 前阻塞（已知限制） |
| Mock 依赖 Schema | `GrpcMockRegistry.update()` 中的 `encodeFromJson()` 依赖 `ProtobufSchemaRegistry` 已加载对应 `.proto`；若 Schema 未就绪，编码失败的规则将被跳过（`encodedBodies` 中无对应 id），不影响其他规则生效 |
| Mock 仅限 HTTP/2 gRPC | 当前 Mock 拦截仅在 `Http2FrontendStreamHandler` 中实现，HTTP/1.1 gRPC-Web 请求不受 Mock 影响 |
| HTTPS 抓包前提 | 客户端（如 Android 设备）必须安装并信任自签 CA 证书，否则前端 TLS 握手失败 |
| EventLoop 线程约束 | `BackendChannelManager` 的所有方法须在同一 EventLoop 线程调用（`withChannel`/`donateChannel`）；`NettyExtensions` 的 `onSuccess` 回调在 EventLoop 线程同步执行，严禁阻塞操作 |
| 线程安全 | Netty pipeline 回调在 EventLoop 线程执行；`callbackFlow.trySend()` 是线程安全的；`ConcurrentHashMap` 保护 `SslContext` 证书缓存 |
