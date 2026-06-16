# PacketCapture 功能模块架构文档

## 1. 模块概览

`packetcapture` 是一个基于 **Netty MITM 代理**实现的 HTTP/HTTPS 流量抓包功能模块。它在本机启动一个本地代理服务器（默认端口 8888），拦截流经该代理的 HTTP 明文请求和 HTTPS 加密请求，并实时呈现在 UI 界面中。

**核心能力**：
- HTTP 明文流量透明转发与捕获
- HTTPS 流量中间人（MITM）解密与捕获（需设备安装自签 CA 证书）
- 动态生成按域名定制的叶子证书（BouncyCastle）
- 实时流量列表展示、关键词过滤、请求详情查看
- 通过 `adb push` 将 CA 证书推送至 Android 设备并提供安装引导

---

## 2. 目录结构

```text
packetcapture/
├── ARCHITECTURE.md                        # 本文件
├── model/
│   ├── CapturedPacket.kt                  # 捕获数据的核心不可变数据类
│   └── CaptureState.kt                    # UI 状态聚合（含过滤、选中等计算属性）
├── data/
│   ├── repository/
│   │   ├── PacketCaptureRepository.kt     # 数据层对外接口
│   │   └── PacketCaptureRepositoryImpl.kt # 数据层实现（聚合 Source 与外部依赖）
│   └── source/
│       ├── CertificateAuthority.kt        # CA 证书管理与按 host 动态签发叶子证书
│       ├── MitmProxyServer.kt             # Netty 服务器入口（生命周期管理）
│       ├── ProxyFrontendHandler.kt        # HTTP/HTTPS CONNECT 路由与前端处理
│       ├── HttpsMitmHandler.kt            # HTTPS MITM：请求侧拦截与转发
│       └── BackendResponseHandler.kt      # HTTPS MITM：后端响应侧接收与回写
└── ui/
    ├── PacketCaptureScreen.kt             # 主屏幕 Composable（布局骨架与对话框）
    ├── PacketCaptureViewModel.kt          # 状态容器（StateFlow + viewModelScope）
    └── components/
        ├── CaptureToolbar.kt              # 工具栏（开始/停止/清空/过滤/CA 安装）
        ├── PacketListPanel.kt             # 左侧请求列表（LazyColumn + 自动滚动）
        └── PacketDetailPanel.kt           # 右侧详情面板（概览/请求头/请求体/响应头/响应体）
```

**外部集成**：`AppScreen.kt` 中注册路由 `AppScreen.PacketCapture`，通过 `PacketCaptureScreen()` 渲染本模块。

---

## 3. 分层架构

本模块严格遵循 [Clean Architecture 三层规范](../../../../../../../../../../docs/desktop_rules.md)，依赖方向自上而下，数据流向自下而上：

```
┌─────────────────────────────────────────┐
│               界面层 (UI)                │
│  PacketCaptureScreen / ViewModel        │
│  CaptureToolbar / PacketListPanel /     │
│  PacketDetailPanel                      │
├─────────────────────────────────────────┤
│              数据层 (Data)               │
│  PacketCaptureRepository（接口边界）     │
│  PacketCaptureRepositoryImpl            │
│  ├── MitmProxyServer                    │
│  │   ├── ProxyFrontendHandler           │
│  │   │   ├── HttpsMitmHandler           │
│  │   │   └── BackendResponseHandler     │
│  └── CertificateAuthority              │
├─────────────────────────────────────────┤
│              模型层 (Model)              │
│  CapturedPacket / CaptureState          │
│  CaptureStatus（被所有层共享）           │
└─────────────────────────────────────────┘
```

> 本模块无 Domain 层（无需 UseCase），`Repository` 直接作为 `ViewModel` 的下层依赖。

---

## 4. 核心组件详解

### 4.1 模型层 (`model/`)

| 类 | 职责 |
|---|---|
| `CapturedPacket` | 单条抓包记录，包含请求（method、headers、body）与响应（statusCode、headers、body、durationMs）字段；不可变 `data class`，仅暴露计算属性（`url`、`isComplete`、`statusText`、`requestBodyAsText()`等）；以 `id` 作为标识符 |
| `CaptureState` | UI 层的单一状态源，聚合 `status`、`port`、`packets`、`selectedPacketId`、`filterText` 等字段；`filteredPackets` 与 `selectedPacket` 为在 model 层计算的派生属性，避免 ViewModel 和 UI 层重复逻辑 |
| `CaptureStatus` | `STOPPED / RUNNING / ERROR` 三态枚举 |

### 4.2 数据层 (`data/`)

#### Repository

- **`PacketCaptureRepository`**（接口）：声明 `startCapture(port): Flow<CapturedPacket>`、`stopCapture()`、`installCaToDevice()`、`getDeviceProxy()`、`getCaCertPath()` 五个合约。`ViewModel` 仅依赖接口，便于测试替换。
- **`PacketCaptureRepositoryImpl`**（实现）：
  - 通过 `callbackFlow` 桥接 Netty 回调与 Kotlin 协程。
  - 持有 `MitmProxyServer` 引用，负责生命周期管理（`start` / `stop`）。
  - 通过注入的 `ProxyRepository` 读取当前设备代理配置。
  - 通过 `adb push` + `CertificateAuthority` 完成 CA 证书推送。

#### Source

| 类 | 职责 |
|---|---|
| `CertificateAuthority` | **单例**。CA 证书持久化到 `{CACHE_HOME}/ca/`（初次启动自动生成，10 年有效期）；按 host 动态签发叶子证书（1 年有效期）并缓存到 `ConcurrentHashMap<String, SslContext>`；依赖 BouncyCastle |
| `MitmProxyServer` | Netty `ServerBootstrap` 封装，管理 `bossGroup` / `workerGroup` 生命周期；以 `AtomicLong` 生成全局单调递增的 packet id；最大请求体限制 **10 MB** |
| `ProxyFrontendHandler` | 区分 `CONNECT` 与普通 HTTP 请求并路由：HTTP 直接透明转发并捕获；CONNECT 则执行完整 MITM 建立流程（见第 5 节） |
| `HttpsMitmHandler` | HTTPS MITM 请求侧，将请求转发至后端并记录到 `ArrayDeque<PendingRequest>`（FIFO 队列维持请求顺序） |
| `BackendResponseHandler` | HTTPS MITM 响应侧，从 `PendingRequest` 队列头部弹出对应请求，组装完整 `CapturedPacket` 并回调 |

### 4.3 界面层 (`ui/`)

| 类 | 职责 |
|---|---|
| `PacketCaptureViewModel` | 持有 `MutableStateFlow<CaptureState>`；`startCapture()` 在 `Dispatchers.IO` 上收集 `Flow<CapturedPacket>` 并 `update` 状态；监听 `Context.stream` 以响应设备切换并刷新代理显示；`onCleared()` 确保代理服务器随生命周期停止 |
| `PacketCaptureScreen` | 顶层骨架 Composable：`Column( CaptureToolbar / Row( PacketListPanel + VerticalDivider + PacketDetailPanel | EmptyDetailPanel ) )`；托管错误对话框与 CA 安装引导对话框 |
| `CaptureToolbar` | 状态驱动的「开始」/「停止」按钮（颜色随 `CaptureStatus` 变化）；搜索框过滤；「安装CA证书」入口 |
| `PacketListPanel` | `LazyColumn` 实现虚拟滚动；新数据到来时自动 `animateScrollToItem` 至末尾；HTTP Method 与状态码均以语义色彩区分 |
| `PacketDetailPanel` | `ScrollableTabRow` 五标签页（概览 / 请求头 / 请求体 / 响应头 / 响应体）；Body 标签自动检测 `Content-Type` 进行 JSON 美化展示（`kotlinx.serialization`）；双向滚动支持 |

---

## 5. HTTPS MITM 核心流程

### 5.1 CONNECT 握手与管道建立

```
客户端发送 CONNECT {host}:443 HTTP/1.1
         │
         ▼
ProxyFrontendHandler.handleConnect()
         │
         ├─ 1. 连接真实后端（TCP + TLS，InsecureTrustManagerFactory）
         │
         ├─ 2. 后端 TLS 握手成功后向客户端回复 200 Connection Established
         │
         ├─ 3. 移除前端 HTTP 编解码器，注入 CertificateAuthority 为 {host}
         │      签发的 SslContext（伪造证书）
         │
         ├─ 4. 前端 TLS 握手成功（客户端需已安装自签 CA）
         │
         └─ 5. 调用 setupMitmPipeline() 完成双向管道建立
```

### 5.2 MITM 管道结构（CONNECT 成功后）

```
[客户端] ←── frontendChannel ──→ [ProxyServer]
  Pipeline:
    frontendSsl          ← CertificateAuthority 动态叶子证书
    httpServerCodecMitm
    httpAggregatorMitm
    HttpsMitmHandler     ← 记录请求到 ArrayDeque<PendingRequest>，转发至后端

[ProxyServer] ←── backendChannel ──→ [真实服务器]
  Pipeline:
    backendSsl           ← InsecureTrustManagerFactory
    httpClientCodec
    httpAggregatorBackend
    BackendResponseHandler ← 从 ArrayDeque 弹出请求，组装 CapturedPacket 并回调

共享状态：ArrayDeque<PendingRequest>（FIFO，按序关联请求与响应）
```

### 5.3 运行时数据流

```
用户点击「开始」
  → ViewModel.startCapture()
  → Repository.startCapture(port) [callbackFlow]
  → MitmProxyServer.start(port)   [Netty bind]
      │
      ├─ HTTP 请求到达
      │    → ProxyFrontendHandler.handleHttp()
      │    → 连接后端、透传、捕获响应
      │    → CapturedPacket(scheme="http") → callbackFlow.trySend()
      │
      └─ HTTPS CONNECT 请求到达
           → ProxyFrontendHandler.handleConnect()
           → [MITM 管道建立，见 5.1/5.2]
           → HttpsMitmHandler → BackendResponseHandler
           → CapturedPacket(scheme="https") → callbackFlow.trySend()
                │
                ▼
  ViewModel._uiState.update { packets + packet }
  UI 列表实时追加并滚动至末尾
```

---

## 6. 外部依赖

| 依赖 | 用途 |
|---|---|
| **Netty** (`io.netty.*`) | HTTP/HTTPS 代理服务器、事件循环、Pipeline、SSL 处理 |
| **BouncyCastle** (`org.bouncycastle.*`) | CA 证书与叶子证书动态生成、PEM 格式读写 |
| **kotlinx.serialization.json** | 响应体 JSON 美化展示 |
| `sophon.desktop.core.Shell.simpleShell` | `adb push` 推送 CA 证书到设备 |
| `sophon.desktop.feature.proxy.data.repository.ProxyRepository` | 读取当前设备代理配置 |
| `sophon.desktop.core.Context.stream` | 监听设备切换事件，刷新代理信息显示 |
| `sophon.desktop.core.CACHE_HOME` | CA 证书文件的持久化目录（`{CACHE_HOME}/ca/`） |

---

## 7. 设计约束与注意事项

| 约束 | 说明 |
|---|---|
| 最大请求体 10 MB | `HttpObjectAggregator(10 * 1024 * 1024)`，超出时 Netty 返回 `413 Request Entity Too Large` |
| CA 证书有效期 10 年 | 首次启动生成，持久化复用；如需更新需手动删除 `{CACHE_HOME}/ca/` 目录 |
| 叶子证书有效期 1 年 | 按 host 缓存在内存 `ConcurrentHashMap`，重启后重新生成 |
| 未暴露端口编辑 UI | `ViewModel.updatePort()` 与 `Repository.getCaCertPath()` 已实现但当前 UI 未提供入口 |
| HTTPS 抓包前提 | 客户端（如 Android 设备）必须安装并信任自签 CA 证书，否则前端 TLS 握手失败，连接被静默关闭 |
| 线程安全 | Netty pipeline 回调在 EventLoop 线程执行，`callbackFlow.trySend()` 是线程安全的；`ConcurrentHashMap` 保护证书缓存 |
