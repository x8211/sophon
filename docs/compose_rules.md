# Jetpack Compose 开发规范 (Compose Rules)

本文档规定了在 Sophon 项目中进行 Jetpack Compose (及其跨平台版本 Compose Multiplatform) 开发的统一规范，旨在保证 UI 渲染性能、代码可读性以及跨平台体验的一致性，同时兼顾各平台的特定特性。

## 1. 通用 Compose 规范 (General Rules)

### 1.1 命名规范
- **产生 UI 的组件 (Composable)**: 返回 `Unit` 且产生 UI 的函数，必须使用 **大驼峰命名法 (PascalCase)** 名词或名词短语。例如：`@Composable fun UserProfileBox() { ... }`。
- **返回值的组件**: 如果一个 `@Composable` 函数不产生 UI，而是返回状态或值，必须使用 **小驼峰命名法 (camelCase)**。例如：`@Composable fun rememberUiState(): UiState`。
- **修饰符参数**: `@Composable` 函数如果接受 `Modifier`，必须将其作为第一个可选参数，并默认提供 `Modifier`。例如：`@Composable fun MyButton(modifier: Modifier = Modifier, ...) { ... }`。

### 1.2 状态管理与提升 (State Management & Hoisting)
- **状态提升**: 尽可能将无状态 (Stateless) 与有状态 (Stateful) 组件分离。将状态和回调提升到调用方，使 UI 组件变纯粹，提升复用性和可结构化测试性。
- **ViewModel 获取状态**: UI 页面级别的状态尽量托管给 `ViewModel`。统一使用 `StateFlow` 或 `SharedFlow`，并在 Composable 中进行收集。
- **不可变性**: 状态应该使用不可变数据结构 (`Data Classes`, 使用 `List` 替代 `MutableList`)。避免在 Recomposition 过程中直接修改变量。

### 1.3 重组性能优化 (Recomposition Optimization)
- **`remember` 的使用**: 任何在重组期间需要被记住且创建成本高的对象，必须使用 `remember`。
- **`derivedStateOf`**: 当输入状态频繁改变，但你只关心特定条件触发的输出改变时（如根据滑动列表的 offset 判断是否显示回到顶部按钮），使用 `derivedStateOf` 减少不必要的重组。
- **`@Stable` 和 `@Immutable`**: 如果向 Composable 传递了自定义的复杂数据类、或包含 `var` 的接口、或通常被 Compose 视为不稳定的类型（如 `List`），建议使用 `@Stable` 或 `@Immutable` 注解，或者替换为 kotlinx.collections.immutable，从而帮助编译器跳过重组。
- **避免耗时操作**: 所有网络、IO 及重写逻辑等耗时操作，必须放在协程作用域 (Coroutine Scope) 或 ViewModel 中。永远不要在 Composable 的重组作用域中直接执行会导致阻塞的代码。

### 1.4 副作用 (Side Effects)
- 使用 `LaunchedEffect` 来由于值的变化触发挂起函数（如加载初始化数据，或响应事件执行动画）。
- 使用 `DisposableEffect` 来执行需要注册和解绑的操作（如事件监听器注入，底层系统 API 生命周期绑定）。
- 使用 `SideEffect` 同步非 Compose 管理的状态（使用较少）。避免将副作用代码暴露在单纯的 UI 重组区域内。

---

## 2. Android 平台开发差异 (Android Specifics)

Android 端的开发侧重于处理移动端受限的生命周期管理、多样的设备分辨率与屏幕形态（普通手机/折叠屏/平板），以及 Android 平台特系统的深入集成。

### 2.1 生命周期与状态保存 (Lifecycle & State Restoration)
- **跨配置保存**: 在 Android 转屏、暗黑模式切换等配置更改时，普通的 `remember` 会丢失状态。如果有需要跨越配置更改保存的简单界面状态（如输入框文本），请使用 `rememberSaveable`。
- **生命周期感知型数据收集**: 当使用 Flow 时，为了防止应用在后台继续收集状态浪费电量与资源，必须使用 `collectAsStateWithLifecycle()`（取代普通的 `collectAsState()`）。
