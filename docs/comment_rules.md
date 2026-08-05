# Kotlin 注释规范

## 核心原则

注释只解释代码本身无法传达的信息：**意图、约束、权衡**。
不要用注释复述代码已经表达清楚的内容。

## 禁止写的注释（冗余注释）

```kotlin
// ❌ 复述代码逻辑
// 将字符串转换为大写
val upper = input.uppercase()

// ❌ 描述函数做什么（函数名已经说明）
// 返回结果
return result

// ❌ 标注明显的代码结构
// 导入依赖
import sophon.desktop.core.Shell

// ❌ 说明正在执行的操作
// 更新状态
_uiState.update { it.copy(isLoading = true) }
```

## 应该写的注释

```kotlin
// ✅ 解释非直觉的技术约束
// bundletool build-apks 要求输出文件不存在，先占位取名再立即删除
val apksOutput = Files.createTempFile("sophon-aab", ".apks").toFile().also { it.delete() }

// ✅ 说明设计取舍
// 使用 object 单例避免 ProGuard 混淆后 clinit 多次触发导致重复创建 DataStore 实例
object DataStoreProvider { ... }

// ✅ 警示易错的副作用或边界条件
// selectedDevice 为空字符串而非 null，需用 isNotBlank() 判断
if (serial.isNotBlank()) append(" --device-id=$serial")

// ✅ 解释"为什么不用更直观的做法"
// 不直接使用 ProcessBuilder，统一走 Shell 单例以适配多平台命令格式差异
"adb devices".streamShell()
```

## KDoc 规范

- **公开 API**（`public` / `internal` 的类、接口、顶层函数）须写 KDoc
- **私有实现**无需 KDoc，除非逻辑复杂
- KDoc 描述**行为与约定**，不描述实现步骤

```kotlin
// ✅ 描述行为与约定
/**
 * 若命令以 "adb" 开头，自动注入已选设备、平台适配和 adb 路径前缀。
 */
fun formatIfAdbCmd(input: String): String { ... }

// ❌ 描述实现步骤
/**
 * 首先检查字符串是否以 adb 开头，
 * 然后获取 Context 中的设备状态，
 * 接着替换命令前缀...
 */
```

## 语言

注释使用中文，技术专有名词（API、Flow、StateFlow、ViewModel 等）保留英文原文。
