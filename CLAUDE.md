# Sophon 项目编码总纲

本文件是 AI 辅助编码的入口文档，汇总项目的完整架构规范与编码约定。

---

## 1. 项目概览 (Project Overview)

**Sophon** 是一个基于 **Kotlin Multiplatform (KMP)** + **Compose Multiplatform** 构建的 **Desktop 工具软件**，目标平台为 **Desktop (macOS/Windows/JVM)**。

---

## 2. 项目结构 (Project Structure)

项目以 `composeApp` 作为唯一顶层 Gradle 模块：

```text
sophon/
├── composeApp/                    # 主应用模块
│   └── src/
│       ├── desktopMain/           # Desktop 平台全部实现
│       └── desktopTest/           # Desktop 单元测试
│
├── gradle/
│   └── libs.versions.toml         # 统一依赖版本声明（Version Catalog）
└── docs/                          # 架构文档（功能模块级 ARCHITECTURE.md）
```

`desktopMain` 内部按 **Feature-based** 策略组织：

```text
composeApp/src/desktopMain/kotlin/sophon/desktop/
├── feature/                # 业务功能模块，按功能垂直切分
│   ├── [feature_name]/     # 每个功能模块内部按官方架构分层
│   │   ├── model/          # 业务模型 (App Model/Domain Model)
│   │   ├── data/           # 数据层 (Data Layer: Repository, DataSource)
│   │   │   ├── repository/ # 仓库实现 (对外暴露共享数据)
│   │   │   └── source/     # 数据源 (如 API, DB, Shell 终端封装)
│   │   ├── domain/         # 网域层 (Domain Layer: 仅存放 UseCases)
│   │   └── ui/             # 界面层 (UI Layer)
│   │       ├── [Feature]Screen.kt    # Compose 页面 (UI 元素)
│   │       └── [Feature]ViewModel.kt # 状态容器 (State Holder)
├── ui/                     # 通用 UI 组件与系统
│   ├── components/         # 全局通用的原子组件 (Buttons, Dialogs)
│   └── theme/              # 主题配置 (Material 3)
├── core/                   # 核心基础库 (Shell, Context, SocketClient)
│   ├── Context.kt          # 全局核心状态单例 (ADB, 设备管理)
│   └── ...
├── datastore/              # 数据持久化存储
├── pb/                     # 协议定义 (Protocol Buffers)
├── AppScreen.kt            # 应用主路由/导航容器
└── main.kt                 # 桌面端程序入口
```

---

## 3. 核心架构原则 (Architecture Principles)

### 分层原则

每个 feature 内部遵循 **Clean Architecture**，围绕两个方向性原则组织代码：

- **依赖方向（自上而下）**：`界面层 (UI) → 网域层 (Domain) → 数据层 (Data)`，上层可依赖下层，反之禁止。
- **数据流向（自下而上）**：数据从数据层产生，经网域层处理后流向界面层渲染。

### 各层职责

1. **共享模型 (App Models)**
    - **位置**：存放于模块根级的 `model/` 目录下（跨模块复用的模型放入 `core/model`）。
    - **职责**：作为跨层流转的标准数据结构，是 UI、Domain、Data 三层共同识别的核心语言。

2. **数据层 (Data Layer)**
    - **职责**：提供应用数据并包含绝大部分应用业务逻辑（决定数据如何创建、存储和变更的规则）。通过两种方式对外提供数据：**挂起函数**（一次性增删改查）与**数据流 `Flow`**（持续监听变化）。对外暴露的数据类型必须是**不可变的（Immutable）**，确保单一可信来源（Single Source of Truth）的一致性。
    - **组成**：
        - **仓库 (Repository)**：**数据层对外的唯一入口**。聚合和协调多个数据源，向外统一公开数据。接口与实现均放在 `data/repository/` 中。`ViewModel`、`UseCase` 等上层组件**严禁直接依赖 `DataSource`**，必须通过 `Repository` 访问。
        - **数据源 (Data Source)**：位于 `data/source/` 中，仅负责与特定底层（API、数据库、Shell 调用等）进行细化交互。

3. **网域层 (Domain Layer)**
    - **职责**：可选层。封装**复杂业务逻辑**，或将多个 `ViewModel` 中**可复用的简单逻辑**抽取出来。每个 UseCase 只负责**单一功能**，不含可变状态，须保证**主线程安全**。
    - **存储内容**：仅包含各种 `UseCase`。UseCase 不引用 UI 组件，通常以挂起函数或 `Flow` 对外返回结果，依赖 `Repository` 获取数据支撑。UseCase 之间可相互依赖。

4. **界面层 (UI Layer)**
    - **职责**：仅含展示逻辑。收到上游数据后映射为状态在屏幕呈现，以及拦截用户操作向上游发起请求。
    - **组成**：负责 UI 展示的 `@Composable` 及承担状态管理与请求转发职责的 `ViewModel`。

### 其他原则

- **Feature-based 分包**：按功能模块垂直切分，每个 feature 模块内部独立，避免跨模块直接调用实现层。
- **全局状态**：核心全局状态（如 ADB 路径、已连接设备、选中的当前设备）维护在 `sophon.desktop.core.Context` 单例中。页面特定的局部业务状态仍使用各自的 `ViewModel`。
- **集中版本管理**：所有 Gradle 依赖通过 Version Catalog (`libs.versions.toml`) 统一管理，禁止在 `build.gradle.kts` 中硬编码版本号。

---

## 4. 通用编码规范

- **Compose UI**：命名与状态、性能优化等规范，详见 [docs/compose_rules.md](docs/compose_rules.md)。
- **注释规范**：何时写注释、KDoc 约定、禁止冗余注释，详见 [docs/comment_rules.md](docs/comment_rules.md)。

---

## 5. UI 开发规范 (UI Development)

- **设计系统 (Design System)**:
    - 严格使用 **Material 3** (`androidx.compose.material3`)。
    - 布局根节点使用 `AppTheme`，禁止硬编码颜色，必须使用 `MaterialTheme.colorScheme`。
    - 尺寸、间距和圆角必须统一引用 `sophon.desktop.ui.theme.Dimens`。

- **页面定义 (Screen Definition)**:
    - **结构**: 页面即 `@Composable` 函数。
    - **路由**: 路由枚举定义在 `AppScreen` 类中，并在 `SophonApp` 的 `NavHost` 中配置导航图。
    - **ViewModel**: 使用 `androidx.lifecycle.viewmodel.compose.viewModel` 获取实例。

- **组件规范 (Component Standards)**:
    - **通用组件**: 放置在 `ui/components`，如 `ToolBar`、`NavigationSideBar`。仅将高复用性、跨功能的组件放入此处。
    - **侧边栏**: 使用 `NavigationSideBar` 作为主导航，支持展开/收起动画。
    - **滚动条**: 桌面端长列表必须添加垂直滚动条（见 `feature/device/ui/ScrollbarModifier.kt`）。

- **桌面端适配 (Desktop Adaptation)**:
    - **鼠标**: 为可交互元素添加 `Modifier.pointerHoverIcon` 或悬停背景色。
    - **窗口**: 使用 `animateContentSize` 处理布局尺寸变化的过渡动画。
    - **布局**: 善用 `Modifier.weight(1f)` 填充剩余空间，避免硬编码宽高。

---

## 6. 状态管理 (State Management)

- **框架**: 使用标准 **AndroidX ViewModel** (`androidx.lifecycle.ViewModel`)。
- **状态暴露**: 统一使用 `StateFlow`。
    - 内部使用 `private val _uiState = MutableStateFlow(...)`。
    - 外部暴露 `val uiState = _uiState.asStateFlow()`。
- **作用域**: 始终使用 `viewModelScope` 管理协程生命周期。

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyFeatureViewModel(
    private val getDataUseCase: GetDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyUiState>(MyUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            try {
                val data = getDataUseCase()
                _uiState.value = MyUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = MyUiState.Error(e.message)
            }
        }
    }
}
```

---

## 7. 平台互操作性 (Platform Interop)

- **Swing**: 除非绝对必要，否则避免直接使用 Swing 组件。
- **资源**: 使用 `compose.resources` 或 `ClassLoader` 加载资源。
- **线程**: 任何 Swing/Window 操作必须在 `Dispatchers.Main` 上执行。

---

## 8. 命名与风格 (Naming & Style)

- **文件命名**:
    - `[Feature]Screen.kt`: UI 页面（`ui/` 包下）。
    - `[Feature]ViewModel.kt`: 状态容器（`ui/` 包下）。
    - `[Feature]Repository.kt`: 数据仓库的接口或具体实现（`data/repository/` 包下）。
    - `[Feature]DataSource.kt`: 对接底层的具体数据源封装（`data/source/` 包下）。
    - `[Action]UseCase.kt`: 声明具体单一行为的业务用例，如 `GetDevicesUseCase.kt`（`domain/` 包下）。
    - `[Feature]Model.kt`: 跨层的业务模型数据类（`model/` 包下）。
- **Composables**: 名词短语（PascalCase），返回 `Unit`，接受 `Modifier` 作为第一个可选参数。
- **Kotlin**: 单行函数优先使用表达式体（Expression Body）。UI State 优先使用 Data Classes。

---

## 9. Shell 命令执行 (Shell Command Execution)

- **工具类**: 统一使用 `sophon.desktop.core.Shell` 单例。
- **扩展函数**: 命令执行逻辑封装在 `String` 的扩展函数中。
- **常用的三种方式**:
    - **`simpleShell()`**: 挂起函数，执行命令并一次性返回 `String` 类型的完整输出。适用于输出量小且非耗时的命令。
      ```kotlin
      import sophon.desktop.core.Shell.simpleShell

      val output = "ls -la".simpleShell()
      ```
    - **`oneshotShell { content -> ... }`**: 挂起函数，执行命令并将完整输出传递给 `transform` 函数进行解析，返回解析后的结果。
      ```kotlin
      import sophon.desktop.core.Shell.oneshotShell

      val processList = "ps aux".oneshotShell { output ->
          output.lines().map { ... }
      }
      ```
    - **`streamShell()`**: 返回 `Flow<String>`，流式获取命令输出。适用于耗时操作或需要实时展示输出的场景。
      ```kotlin
      import sophon.desktop.core.Shell.streamShell

      "ping google.com".streamShell()
          .collect { line -> updateLog(line) }
      ```
- **注意事项**:
    - 内部已指定 `Dispatchers.IO`，无需手动切换线程。
    - 内部已处理 `adb` 命令的格式化（`Shell.formatIfAdbCmd`），并通过 `ShellExecutor` 策略模式自动适配不同操作系统。
    - 避免直接使用 `ProcessBuilder` 或 `Runtime.getRuntime().exec()`，以保持代码统一与可维护性。

---

## 10. 内置工具打包规范 (Embedded Tools Packaging)

Compose Desktop 提供两种不同的资源机制，**必须根据用途选择正确的一种**：

| 机制 | 目录 | 运行时访问方式 | 适用场景 |
|---|---|---|---|
| **类路径资源** | `src/desktopMain/resources/` | `ClassLoader.getResourceAsStream("foo")` 读取为**字节流** | 图标、配置模板、内嵌文本等只需读取内容的资源 |
| **App 资源** | `src/desktopMain/appResources/` | `System.getProperty("compose.application.resources.dir")` 得到**磁盘目录路径** | 可执行文件、JAR 工具、原生二进制等需要以子进程执行的工具 |

### 需要子进程执行的外部工具，必须使用 `appResources/`

**禁止**将工具放入 `resources/` 然后在运行时提取到临时目录再执行。这是多余的 IO 操作，且破坏项目统一性。

**正确做法**：

1. **放置位置**：
   - 跨平台工具（如 JAR）→ `appResources/common/tools/`
   - 平台专属二进制 → `appResources/macos/tools/` 或 `appResources/windows/tools/`

2. **路径解析模式**（参考 [`EmbeddedProtoc.kt`](composeApp/src/desktopMain/kotlin/sophon/desktop/feature/packetcapture/data/source/grpc/EmbeddedProtoc.kt) 和 [`EmbeddedBundletool.kt`](composeApp/src/desktopMain/kotlin/sophon/desktop/feature/installaab/data/source/EmbeddedBundletool.kt)）：

   ```kotlin
   internal object EmbeddedMyTool {
       val path: String by lazy {
           // 打包模式：Compose 将 appResources 平铺到 resources.dir 下
           val resourcesDir = System.getProperty("compose.application.resources.dir")
           if (resourcesDir != null) {
               val f = File(resourcesDir, "tools/my-tool")
               if (f.exists()) return@lazy f.absolutePath
           }
           // 开发模式：直接指向源目录
           listOf(
               "composeApp/src/desktopMain/appResources/common/tools/my-tool",
               "src/desktopMain/appResources/common/tools/my-tool",
           ).firstOrNull { File(it).exists() } ?: "tools/my-tool"
       }
   }
   ```

3. **可执行权限**：原生二进制在打包后可能丢失执行位，需调用 `file.setExecutable(true)`（参考 `EmbeddedProtoc.ensureExecutable`）；JAR 文件无需此步骤。

4. **build.gradle.kts** 已配置 `appResourcesRootDir`，`common/`、`macos/`、`windows/` 子目录会在对应平台打包时自动合并进安装包，无需额外配置。

---

## 11. 其他规范 (Other Standards)

- **禁止过时方法**: 不要使用过时（Deprecated）的方法，应立即更换为系统或 IDE 建议的新方法（ReplaceWith）。
- **ProGuard 混淆规则**: release 包混淆配置见 `composeApp/proguard-rules.pro`。

---

## 12. 功能模块架构文档索引 (Feature Architecture Docs)

各功能模块如含复杂实现（如网络协议、多组件协作、特殊生命周期管理），须在其目录下维护 `ARCHITECTURE.md`，记录分层结构、核心流程与设计约束。

| 功能模块 | 架构文档 | 核心能力摘要 |
|---|---|---|
| packetcapture | [ARCHITECTURE.md](composeApp/src/desktopMain/kotlin/sophon/desktop/feature/packetcapture/ARCHITECTURE.md) | 基于 Netty MITM 代理的 HTTP/HTTPS 流量抓包；BouncyCastle 动态签发叶子证书；callbackFlow 桥接 Netty 回调与协程 |
