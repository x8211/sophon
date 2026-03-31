
# =============================================================================
# Sophon Desktop - ProGuard 混淆规则
# =============================================================================

# -----------------------------------------------------------------------------
# 规则 1: sophon.desktop 包下的所有类均不混淆
# 保留类名、方法名、字段名及所有属性
# -----------------------------------------------------------------------------
-keep class sophon.desktop.** { *; }
-keepnames class sophon.desktop.**
-keepclassmembers class sophon.desktop.** { *; }

# -----------------------------------------------------------------------------
# 通用规则: Kotlin 基础支持
# -----------------------------------------------------------------------------

# 保留 Kotlin 元数据注解（反射所必需）
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# 保留 Kotlin 协程核心类
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 保留 Kotlin 序列化（JSON 反序列化依赖反射）
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-dontwarn kotlinx.serialization.**

# 保留 Kotlin 标准库
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# 保留 Kotlin 反射
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# 保留所有枚举类（防止 Enum.valueOf/values() 被移除，避免运行时异常）
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public **[] $values();
    public static ** $values();
    **[] $VALUES;
    public static synthetic ** valueOf(int);
}
-keep enum * { *; }

# -----------------------------------------------------------------------------
# 通用规则: Jetpack Compose & Compose Multiplatform
# -----------------------------------------------------------------------------

# 保留 Compose 运行时
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 保留 Compose 导航
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# 保留 AndroidX Lifecycle（ViewModel、StateFlow 等）
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# 保留 DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# 保留带有 @Composable 注解的类/方法
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# -----------------------------------------------------------------------------
# 通用规则: JVM & 标准 Java 类库
# -----------------------------------------------------------------------------

# 保留所有实现 Serializable 的类（Java 序列化）
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留所有实现 Parcelable 的类（若有）
-keep class * implements android.os.Parcelable { *; }

# 不混淆主入口类（compose desktop 需要）
-keep class sophon.desktop.MainKt { *; }

# 不打印任何警告（第三方库可能有残留引用）
-dontwarn **

# 保留自定义异常类（便于 Crashlytics 分析堆栈）
-keep public class * extends java.lang.Exception

# 保留反射所需的类名（ProGuard 会移除构造函数参数名）
-keepparameternames

# -----------------------------------------------------------------------------
# 通用规则: 日志与调试（线上包可选择关闭）
# -----------------------------------------------------------------------------

# 如需在 release 包中移除 log，取消注释以下规则：
# -assumenosideeffects class java.io.PrintStream {
#     public void println(java.lang.String);
#     public void println(java.lang.Object);
# }

-keep class org.sqlite.** { *; }

