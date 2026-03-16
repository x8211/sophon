---
trigger: always_on
glob:
description: Sophon 项目 AI 编码总纲
---

# Sophon 项目编码总纲

本文件是 AI 辅助编码的入口文档，汇总项目的整体架构概览与各平台子规范的索引。

---

## 1. 项目概览 (Project Overview)

**Sophon** 是一个基于 **Kotlin Multiplatform (KMP)** + **Compose Multiplatform** 构建的多平台工具软件，当前主要支持 **Desktop (macOS/JVM)** 和 **Android** 两个目标平台。

---

## 2. 项目结构 (Project Structure)

项目包含两个顶层 Gradle 模块，内部按照 KMP **Source Set** 进行平台区分：

```text
sophon/
├── composeApp/                    # 主应用模块：含全部平台的 UI 与业务逻辑
│   └── src/
│       ├── commonMain/            # 跨平台共享 UI 代码（Compose，资源等）
│       ├── desktopMain/           # Desktop 平台特定实现
│       ├── androidMain/           # Android 平台特定实现
│       └── desktopTest/           # Desktop 单元测试
│
├── shared/                        # 共享逻辑模块：纯 Kotlin，无 Compose 依赖
│   └── src/
│       ├── commonMain/            # 全平台通用的纯逻辑（算法、模型、工具）
│       ├── androidMain/           # Android 平台适配实现
│       ├── desktopMain/           # Desktop 平台适配实现
│       └── iosMain/               # iOS 平台适配实现
│
├── gradle/
│   └── libs.versions.toml         # 统一依赖版本声明（Version Catalog）
├── docs/                          # 编码规范与架构文档
└── .agent/rules/AGENTS.md         # AI 编码总纲（本文件）
```

### composeApp/commonMain vs shared/commonMain

这是两个最容易混淆的目录，职责有本质区别：

| 维度       | `composeApp/commonMain`                   | `shared/commonMain`               |
|----------|-------------------------------------------|-----------------------------------|
| **定位**   | 各平台共享的 **UI 与应用层** 代码                     | 各平台共享的 **纯逻辑层** 代码                |
| **允许依赖** | Compose Multiplatform、UI 框架、`shared` 模块   | 纯 Kotlin、KMP 标准库，**禁止依赖 Compose** |
| **典型内容** | 公共 Composable 入口、跨平台资源（字体/图片/字符串）、跨平台工具类  | 业务算法、数据模型、平台无关工具函数                |
| **消费方**  | 各平台 Source Set（desktopMain、androidMain 等） | `composeApp` 模块及其各平台 Source Set   |

> **选择原则**：新代码如果不含任何 UI/Compose 依赖，且可以被 iOS 等平台复用，优先放到 `shared/commonMain`；否则放到 `composeApp/commonMain`。

---

## 3. 核心架构原则 (Architecture Principles)

1. **KMP Source Set 隔离**：公共逻辑优先放入 `commonMain`，只有真正存在平台差异的代码才下沉到各平台 Source Set。
2. **Feature-based 分包**：各平台按功能模块垂直切分，每个 feature 模块内部独立，避免跨模块直接调用实现层。
3. **Clean Architecture DIP**：在每个 feature 内部，编译时依赖方向为 `UI → Domain ← Data`，Domain 层不依赖任何框架。
4. **集中版本管理**：所有 Gradle 依赖通过 Version Catalog (`libs.versions.toml`) 统一管理，禁止在 `build.gradle.kts` 中硬编码版本号。

---

## 4. 平台规范索引 (Platform Rules Index)

各平台的详细编码规范请查阅对应文档：

| 平台                  | 规范文档                                            | 主要内容                          |
|---------------------|-------------------------------------------------|-------------------------------|
| Compose (UI共享)      | [compose_rules.md](../../docs/compose_rules.md) | 命名与状态、性能优化、Android/Desktop差异  |
| Desktop (JVM/macOS) | [desktop_rules.md](../../docs/desktop_rules.md) | 项目结构、UI 规范、状态管理、Shell 执行、命名规范 |
| Android             | *(待补充)*                                         | —                             |
| iOS                 | *(待补充)*                                         | —                             |
| Common/Shared       | *(待补充)*                                         | —                             |