# Desktop (desktopMain) 平台编码规范

## 1. 项目结构与模块划分 (Project Structure)

遵循 **Feature-based**（按功能分包）且内部应用 **Clean Architecture** 的策略。

```text
desktopMain/kotlin/sophon/desktop/
├── feature/                # 业务功能模块，按功能垂直切分
│   ├── [feature_name]/     # 功能模块内部采用 Clean Architecture
│   │   ├── domain/         # 领域层：业务逻辑与抽象
│   │   │   ├── model/      # 领域模型 (Data Class)
│   │   │   ├── repository/ # 仓库接口定义
│   │   │   └── usecase/    # 具体的业务用例 (UseCases)
│   │   ├── data/           # 数据层：实现细节
│   │   │   └── repository/ # 仓库接口实现 (Impl)
│   │   └── ui/             # 界面层：展示逻辑
│   │       ├── [Feature]Screen.kt    # Compose 页面
│   │       └── [Feature]ViewModel.kt # ViewModel
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

### 规则 (Rules)
- **各层职责**:
    - **Domain 层**: 必须是纯 Kotlin 库，不应依赖 Android 或 Desktop 框架类（如 `Context`, `compose`）。
    - **Data 层**: 负责数据获取的具体实现，如 Shell 命令调用、网络请求或本地存储。
    - **UI 层**: 仅负责渲染 UI 和处理用户交互，通过 UseCase 与 Domain 层通信。
- **新功能**: 在 `feature/` 下创建独立包，并按上述结构划分三层。
- **依赖方向**: 依赖关系必须是单向的：`UI -> Domain <- Data` (UI 和 Data 都依赖于 Domain)。
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
    - `[Feature]Screen.kt`: UI 页面。
    - `[Feature]ViewModel.kt`: UI 逻辑。
    - `[Feature]Repository.kt`: 接口定义（放在 domain）。
    - `[Feature]RepositoryImpl.kt`: 接口实现（放在 data）。
    - `[Feature]UseCase.kt` 或 `DoSomethingUseCase.kt`: 业务用例。
    - `[Feature]Model.kt`: 领域模型。
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
