# Desktop (desktopMain) 平台编码规范

## 1. 项目结构与模块划分 (Project Structure)

遵循 **Feature-based**（按功能分包）的策略，同时严格参考 **Android 官方架构指南 (Modern App Architecture)** 与 **NowInAndroid (NiA)** 的架构演进，围绕不同关注点将应用划分为不同层级。

```text
desktopMain/kotlin/sophon/desktop/
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

### 各层职责与规范 (Layers & Responsibilities)

架构遵循以下两个方向性原则：
- **依赖方向（自上而下）**：`界面层 (UI) → 网域层 (Domain) → 数据层 (Data)`。上层组件可以依赖下层，反之禁止。
- **数据流向（自下而上）**：`数据层 (Data) → 网域层 (Domain) → 界面层 (UI)`。数据与状态从数据层产生，经网域层处理后，最终流向界面层进行渲染。

1. **共享模型 (App Models)**
    - **位置**：存放于模块根级的 `model/` 目录下（或针对跨模块复用的模型放入专门的 `core/model` 中）。
    - **职责**：作为跨层流转的标准数据结构，业务模型是 UI、Domain、Data 三层共同识别的核心语言。

2. **数据层 (Data Layer)**
    - **职责**：提供应用数据并包含绝大部分**应用业务逻辑**（即决定数据如何创建、存储和变更的业务规则）。该层负责管理单一可信来源（Single Source of Truth），通过两种方式对外提供数据：**挂起函数**（用于一次性的增删改查操作）与**数据流 `Flow`**（用于持续监听数据变化）。该层对外暴露的数据类型必须是**不可变的（Immutable）**，防止上层直接修改数据，确保单一可信来源的一致性。
    - **组成**：
        - **仓库 (Repository)**：**数据层对外的唯一入口**。负责聚合和协调多个数据源，向应用其余部分统一公开数据。它的接口和实现类均归属在 `data/repository/` 中（若无多态需也可直接提供具体类）。`ViewModel`、`UseCase` 等上层组件严禁直接依赖 `DataSource`，必须通过 `Repository` 访问数据。
        - **数据源 (Data Source)**：位于 `data/source/` 中，仅负责与特定的底层（例如 API、数据库实例、Shell 系统调用）进行细化交互。

3. **网域层 (Domain Layer)**
    - **职责**：属于可选层。介于界面层和数据层之间，主要负责封装**复杂的业务逻辑**，或者将多个 `ViewModel` 中**可复用的简单逻辑**抽取出来。每个 UseCase 只负责**单一功能**，不应包含可变数据（mutable state）——可变状态应由 UI 层或数据层管理。
    - **存储内容**：非常纯粹，仅包含各种各样的 `UseCase`。UseCase 不能引用 UI 组件，通常以挂起函数或数据流 `Flow` 的形式对外返回结果，须保证**主线程安全**。依赖于 `data` 层的 `Repository` 获取数据支撑。UseCase 之间也可以相互依赖——当某个复杂用例需要复用其他用例的逻辑时，可以将其他 UseCase 作为依赖注入。

4. **界面层 (UI Layer)**
    - **职责**：仅包含展示逻辑。负责在收到上游（Data/Domain）数据后映射为状态在屏幕呈现，以及拦截用户操作向上游发起请求。
    - **组成**：负责 UI 展示的 `@Composable` 及承担状态管理与请求转发职责的 `ViewModel`。

- **组件**: 仅将高复用性、跨功能的组件放入 `ui/components`。
- **全局状态**: 核心全局状态（如 ADB 路径、已连接设备、选中的当前设备）维护在 `sophon.desktop.core.Context` 单例中。页面特定的局部业务状态仍使用各自的 `ViewModel`。

## 2. UI 开发规范 (UI Development)

- **设计系统 (Design System)**:
    - 严格使用 **Material 3** (`androidx.compose.material3`)。
    - 布局根节点使用 `AppTheme`，禁止硬编码颜色，必须使用 `MaterialTheme.colorScheme`。
    - 尺寸、间距和圆角必须统一引用 `sophon.desktop.ui.theme.Dimens`。

- **页面定义 (Screen Definition)**:
    - **结构**: 页面即 `@Composable` 函数。
    - **路由**: 路由枚举定义在 `AppScreen` 类中，并在 `SophonApp` 的 `NavHost` 中配置导航图。
    - **ViewModel**: 使用 `androidx.lifecycle.viewmodel.compose.viewModel` 获取实例。

- **组件规范 (Component Standards)**:
    - **通用组件**: 放置在 `ui/components`，如 `ToolBar`, `NavigationSideBar`。
    - **侧边栏**: 使用 `NavigationSideBar` 作为主导航，支持展开/收起动画。
    - **滚动条**: 桌面端长列表必须添加垂直滚动条 (见 `feature/device/ScrollbarModifier.kt`)。

- **桌面端适配 (Desktop Adaptation)**:
    - **鼠标**: 为可交互元素添加 `Modifier.pointerHoverIcon` 或悬停背景色。
    - **窗口**: 使用 `animateContentSize` 处理布局尺寸变化的过渡动画。
    - **布局**: 善用 `Modifier.weight(1f)` 填充剩余空间，避免硬编码宽高。

## 3. 状态管理 (State Management)

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
    private val getDataUseCase: GetDataUseCase // 注入 UseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyUiState>(MyUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch { 
            try {
                val data = getDataUseCase() // 调用领域层逻辑
                _uiState.value = MyUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = MyUiState.Error(e.message)
            }
        }
    }
}
```

## 4. 平台互操作性 (Platform Interop)

- **Swing**: 除非绝对必要，否则避免直接使用 Swing 组件。
- **资源**: 使用 `compose.resources` 或 `ClassLoader` 加载资源。
- **线程**: 任何 Swing/Window 操作必须在 `Dispatchers.Main` 上执行。

## 5. 命名与风格 (Naming & Style)

- **文件命名**:
    - `[Feature]Screen.kt`: UI 页面 (`ui/` 包下)。
    - `[Feature]ViewModel.kt`: 状态容器 (`ui/` 包下)。
    - `[Feature]Repository.kt`: 数据仓库的接口或具体层级实现 (`data/repository/` 包下)。
    - `[Feature]DataSource.kt`: 对接底层的具体数据源封装 (`data/source/` 包下)。
    - `[Action]UseCase.kt`: 声明具体单一行为的业务用例 (如 `GetDevicesUseCase.kt`，`domain/` 包下)。
    - `[Feature]Model.kt`: 跨层的业务模型数据类 (`model/` 包下)。
- **Composables**:名词短语（PascalCase），返回 `Unit`，接受 `Modifier` 作为第一个可选参数。
- **Kotlin**: 单行函数优先使用表达式体 (Expression Body)。UI State 优先使用 Data Classes。

## 6. 依赖管理 (Dependencies)

- 所有依赖项必须在 `gradle/libs.versions.toml` 中定义。
- **严禁**在 `build.gradle.kts` 中硬编码版本号。
- 确保 `desktopMain` 的依赖项兼容 KMP 或特定于 Desktop (JVM)。

## 7. Shell 命令执行 (Shell Command Execution)

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
    - 内部已处理 `adb` 命令的格式化 (`Context.formatIfAdbCmd`)。
    - 避免直接使用 `ProcessBuilder` 或 `Runtime.getRuntime().exec()`，以保持代码统一与可维护性。

## 8. 其他规范 (Other Standards)

- **禁止过时方法**:不要使用过时 (Deprecated) 的方法，应立即更换为系统或 IDE 建议的新方法 (ReplaceWith)。
