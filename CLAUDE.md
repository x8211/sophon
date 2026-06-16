# Sophon 项目编码总纲

本文件是 AI 辅助编码的入口文档，汇总项目的整体架构概览与各平台子规范的索引。

---

## 1. 项目概览 (Project Overview)

**Sophon** 是一个基于 **Kotlin Multiplatform (KMP)** + **Compose Multiplatform** 构建的多平台工具软件，当前主要支持 **Desktop (macOS/Windows/JVM)** 和 **Android** 两个目标平台。

---

## 2. 项目结构 (Project Structure)

项目以 `composeApp` 作为唯一顶层 Gradle 模块，内部按照 KMP **Source Set** 进行平台区分：

```text
sophon/
├── composeApp/                    # 主应用模块：含全部平台的 UI 与业务逻辑
│   └── src/
│       ├── commonMain/            # 跨平台共享代码（Compose 入口、资源等）
│       ├── desktopMain/           # Desktop 平台全部实现（详见 desktop_rules.md）
│       ├── androidMain/           # Android 平台特定实现
│       └── desktopTest/           # Desktop 单元测试
│
├── gradle/
│   └── libs.versions.toml         # 统一依赖版本声明（Version Catalog）
├── docs/                          # 编码规范与架构文档
└── .agent/rules/AGENTS.md         # AI 编码总纲（本文件）
```

> `desktopMain` 内部的详细目录结构与分层约定，请参阅 [desktop_rules.md](docs/desktop_rules.md) 第 1 节。

---

## 3. 核心架构原则 (Architecture Principles)

1. **KMP Source Set 隔离**：公共逻辑优先放入 `commonMain`，只有真正存在平台差异的代码才下沉到各平台 Source Set。
2. **Feature-based 分包**：各平台按功能模块垂直切分，每个 feature 模块内部独立，避免跨模块直接调用实现层。
3. **Clean Architecture 分层**：每个 feature 内部遵循 Android Modern App Architecture，围绕两个方向性原则组织代码：
    - **依赖方向（自上而下）**：`界面层 (UI) → 网域层 (Domain) → 数据层 (Data)`，上层可依赖下层，反之禁止。
    - **数据流向（自下而上）**：数据从数据层产生，经网域层处理后流向界面层渲染。
    - **数据层**：承载绝大多数业务逻辑（数据创建、存储、变更规则），`Repository` 是数据层对外的唯一入口，`ViewModel`/`UseCase` 严禁直接依赖 `DataSource`；对外暴露的数据类型必须是**不可变的（Immutable）**。
    - **网域层**：可选层，仅在需要封装复杂逻辑或跨 `ViewModel` 复用逻辑时引入 `UseCase`；`UseCase` 不含可变状态，须保证主线程安全。
    - **界面层**：仅含展示逻辑，由 `@Composable` 负责渲染、`ViewModel`（`StateFlow`）负责状态持有与请求转发。
4. **集中版本管理**：所有 Gradle 依赖通过 Version Catalog (`libs.versions.toml`) 统一管理，禁止在 `build.gradle.kts` 中硬编码版本号。

---

## 4. 平台规范索引 (Platform Rules Index)

各平台的详细编码规范请查阅对应文档：

| 平台                  | 规范文档                                            | 主要内容                          |
|---------------------|-------------------------------------------------|-------------------------------|
| Compose (UI共享)      | [compose_rules.md](docs/compose_rules.md) | 命名与状态、性能优化、Android/Desktop差异  |
| Desktop (JVM/macOS/Windows) | [desktop_rules.md](docs/desktop_rules.md) | 项目结构、UI 规范、状态管理、Shell 执行、命名规范 |
| Android             | *(待补充)*                                         | —                             |
| Common/Shared       | *(待补充)*                                         | —                             |
