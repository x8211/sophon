@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.16.0")
    compileOnly(libs.ksp)
}

// 配置源代码jar，使注解可以被其他模块使用
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

// 设置Kotlin编译器的JVM目标版本为11，与Java保持一致
kotlin {
    jvmToolchain(11)
}