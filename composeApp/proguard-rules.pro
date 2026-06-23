
# =============================================================================
# Sophon Desktop - ProGuard 混淆规则
# =============================================================================

# -----------------------------------------------------------------------------
# 规则 1: sophon.desktop 包下的所有类均不混淆
# 保留类名与成员，便于 crash 堆栈可读
# （obfuscate=true 仍生效于第三方依赖）
# -----------------------------------------------------------------------------
-keep class sophon.desktop.** { *; }

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

# -----------------------------------------------------------------------------
# 通用规则: 第三方依赖
# -----------------------------------------------------------------------------

# Netty（反射加载 Channel Factory / native transport）
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# BouncyCastle（Security.addProvider 按类名注册 Provider）
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Protobuf（项目使用 DynamicMessage 等运行时 API，无用户生成类）
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

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

# 保留自定义异常类，确保 crash 堆栈中异常类名可读
-keep public class * extends java.lang.Exception

# 保留反射所需的类名（ProGuard 会移除构造函数参数名）
-keepparameternames