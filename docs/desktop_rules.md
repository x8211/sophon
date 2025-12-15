# Desktop (desktopMain) 平台编码规范

## 1. 项目结构与模块划分 (Project Structure)

遵循 **Feature-based**（按功能分包）的策略。

```text
desktopMain/kotlin/sophon/desktop/
├── feature/                # 业务功能模块，按功能垂直切分
│   ├── device/             # 示例：设备信息功能
│   │   ├── DeviceInfoScreen.kt       # 视图 (View)
│   │   ├── DeviceInfoViewModel.kt    # 逻辑 (ScreenModel)
│   │   └── GetPropDataSource.kt      # 数据源 (Data Source)
│   ├── apps/               # 应用管理功能
│   └── ...
├── ui/                     # 通用 UI 组件与系统
│   ├── components/         # 全局通用的原子组件 (Buttons, Dialogs)
│   └── theme/              # 主题配置 (Material 3)
├── core/                   # 核心基础库 (Utils, Global State)
└── AppScreen.kt            # 应用主入口屏幕
```

### 规则 (Rules)
- **新功能**: 在 `feature/` 下创建独立包，包含该功能所需的所有 UI、State 和 Logic。
- **组件**: 仅将高复用性、跨功能的组件放入 `ui/components`。

## 2. UI 开发规范 (UI Development)

- **设计系统**: 严格使用 **Material 3** (`androidx.compose.material3`)。
- **主题**: 所有 UI 必须包裹在 `AppTheme` 中。**严禁**硬编码颜色值，必须使用 `MaterialTheme.colorScheme`。
- **页面定义 (Screen Definition)**:
    - 实现 `cafe.adriel.voyager.core.screen.Screen` 接口。
    - Screen 类应保持轻量（仅负责布局和 ViewModel 绑定）。
    - 将内容提取为 `@Composable private fun ...Content()` 以便预览和复用。
- **桌面端适配 (Desktop Adaptation)**:
    - **鼠标**: 为可交互元素添加悬停效果 (`Modifier.hoverable`, `Modifier.pointerHoverIcon`)。
    - **键盘**: 通过 `KeyboardHandler` 或 `onKeyEvent` 支持快捷键。
    - **窗口**: 处理特定的窗口约束和尺寸限制。

## 3. 状态管理 (State Management)

- **框架**: 使用 **Voyager ScreenModel**。
- **状态暴露**: 使用 `stateIn` 或 `MutableStateFlow`。
    - 简单页面：继承 `StateScreenModel<T>`。
    - 复杂页面：暴露 `val uiState: StateFlow<UiState>`。
- **作用域**: 始终使用 `screenModelScope`。

```kotlin
class MyFeatureViewModel : StateScreenModel<MyState>(MyState.Loading) {
    fun loadData() {
        screenModelScope.launch { /* ... */ }
    }
}
```

## 4. 平台互操作性 (Platform Interop)

- **Swing**: 除非绝对必要，否则避免直接使用 Swing 组件。
- **资源**: 使用 `compose.resources` 或 `ClassLoader` 加载资源。
- **线程**: 任何 Swing/Window 操作必须在 `Dispatchers.Main` 上执行。

## 5. 命名与风格 (Naming & Style)

- **文件命名**:
    - `[Feature]Screen.kt`
    - `[Feature]ViewModel.kt`
    - `[Feature]DataSource.kt`
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
