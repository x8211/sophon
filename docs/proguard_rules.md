# ProGuard 混淆规范

本文档规定 Sophon Desktop release 包的 ProGuard 配置原则，对应文件：`composeApp/proguard-rules.pro`。

---

## 1. 配置入口

`build.gradle.kts` 中的 ProGuard 配置：

```kotlin
buildTypes.release.proguard {
    isEnabled = true
    obfuscate.set(true)   // 对第三方依赖生效；自身代码被 -keep 覆盖（见第 2 节）
    optimize.set(true)
    configurationFiles.from(project.file("proguard-rules.pro"))
}
```

> `obfuscate.set(true)` 仅对**未被 `-keep` 保护的类**生效。`sophon.desktop.**` 包已整体保留类名，目的是保持 crash 堆栈可读。

---

## 2. 核心原则

### 2.1 应用自身代码：整包保留

```proguard
-keep class sophon.desktop.** { *; }
```

**只写一条**，禁止叠加冗余指令：

| 写法 | 是否需要 | 原因 |
|------|---------|------|
| `-keep class sophon.desktop.** { *; }` | 是 | 保留类名 + 成员名 + 成员 |
| `-keepnames class sophon.desktop.**` | **否** | `-keep` 已包含此效果 |
| `-keepclassmembers class sophon.desktop.** { *; }` | **否** | `-keep` 已包含此效果 |

### 2.2 第三方库：必须单独声明

每个使用了**反射、ServiceLoader、动态类加载**的依赖都必须有对应的 `-keep` + `-dontwarn` 对，**不能依赖 `-dontwarn **` 全局兜底**来掩盖缺失规则。

当前项目必须声明的库：

| 库 | 原因 |
|----|------|
| `io.netty.**` | 反射加载 Channel Factory；native transport 探测 |
| `org.bouncycastle.**` | `Security.addProvider()` 按类名注册 Provider |
| `com.google.protobuf.**` | DynamicMessage / Descriptor 依赖反射 |
| `org.sqlite.**` | JDBC 驱动注册 |

**新增依赖时的检查清单**：

- [ ] 该库是否使用 `ServiceLoader` 加载实现类？
- [ ] 该库是否通过类名字符串进行反射实例化？
- [ ] 该库是否有 `META-INF/services/` 服务注册文件？

满足任意一条，即需添加 `-keep class <包名>.** { *; }` + `-dontwarn <包名>.**`。

### 2.3 禁止使用 `-dontwarn **`

全局通配符 `-dontwarn **` 会掩盖第三方库因缺少 `-keep` 规则而产生的警告，导致 release 包运行时崩溃却在构建期间无任何提示。

每个库的 `-dontwarn` 必须**精确声明到包名**：

```proguard
# 正确
-dontwarn io.netty.**
-dontwarn org.bouncycastle.**

# 禁止
-dontwarn **
```

### 2.4 注解的 `-keepclassmembers` 必须验证注解 Target

`-keepclassmembers class * { @SomeAnnotation *; }` 的语义是"保留被该注解标注的**成员**"。

若注解的 `AnnotationTarget` 仅为 `CLASS`（如 `@kotlinx.serialization.Serializable`），则该规则永远不会匹配任何成员，是**死规则**，应删除。

```proguard
# 死规则示例（@Serializable 是 CLASS 级注解，不能标注成员）
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;   ← 永远匹配不到，直接删除
}
```

正确做法：只保留库运行时的整包规则即可。

### 2.5 不引入平台无关规则

项目为 **Desktop JVM**，以下规则无效，禁止出现：

```proguard
# 禁止：Android-only
-keep class * implements android.os.Parcelable { *; }
```

---

## 3. 规则结构模板

新增第三方依赖时，在 `proguard-rules.pro` 的"第三方依赖"节追加：

```proguard
# <库名>（<必须保留的原因：反射/ServiceLoader/Provider注册等>）
-keep class <group.artifact>.** { *; }
-dontwarn <group.artifact>.**
```

若该库有**用户生成类**（如 proto 生成的 Java Message 类）且这些类在 `sophon.desktop.**` 之外，额外追加：

```proguard
-keepclassmembers class * extends <BaseClass> { *; }
```

> 当前项目使用 `DynamicMessage` API，无用户生成的 proto Java 类，无需此条。

---

## 4. 常见陷阱速查

| 陷阱 | 症状 | 解决方式 |
|------|------|---------|
| 缺少 Netty 规则 | release 包启动时 `ClassNotFoundException` 或 native transport 初始化失败 | 添加 `-keep class io.netty.** { *; }` |
| 缺少 BouncyCastle 规则 | TLS 握手失败，`NoSuchProviderException` | 添加 `-keep class org.bouncycastle.** { *; }` |
| 缺少 Protobuf 规则 | `parseFrom` / `DynamicMessage` 调用抛 `ClassNotFoundException` | 添加 `-keep class com.google.protobuf.** { *; }` |
| `-keep` 冗余叠加 | 规则文件臃肿，但无实际危害 | 一个 `-keep` 包含所有效果，删除 `keepnames`/`keepclassmembers` 重复条目 |
| `@Annotation` 成员规则无效 | 死规则，构建正常但规则无效 | 确认注解 `AnnotationTarget` 后决定是否保留 |
| `-dontwarn **` 掩盖问题 | 构建无警告，但 release 包运行崩溃 | 精确声明每个库的 `-dontwarn` |
| `@Composable` 类规则无效 | `-keep @Composable class *` 死规则；`-keepclassmembers @Composable *` 冗余 | `@Composable` 不含 `CLASS` Target，不能标注类；其函数已被 `sophon.desktop.**` 和 `androidx.compose.**` 整包规则覆盖 |
