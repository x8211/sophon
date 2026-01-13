import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

apply {
    from(file(rootProject.layout.projectDirectory.dir("gradle/desktop/package_with_server_dex.gradle.kts")))
    from(file(rootProject.layout.projectDirectory.dir("gradle/app/package_dex_for_desktop.gradle.kts")))
    from(file(rootProject.layout.projectDirectory.dir("gradle/desktop/copy_adb_tools.gradle.kts")))
}

val generateAppInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/appInfo/kotlin")
    val buildTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    val appVersion = compose.desktop.application.nativeDistributions.packageVersion
    inputs.property("version", appVersion)
    inputs.property("buildTime", buildTimeStr)
    outputs.dir(outputDir)

    doLast {
        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val outputFile = outputDir.get().file("sophon/desktop/generated/AppInfo.kt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package sophon.desktop.generated

            object AppInfo {
                const val APP_VERSION = "$appVersion"
                const val BUILD_TIME = "$buildTime"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting{
            kotlin.srcDir(generateAppInfo.map { it.outputs.files.asPath })
        }
        val desktopTest by getting // 添加测试源集

        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(libs.serialization.json)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(project(":desktop-processor"))
        }
        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.engine)
            implementation(libs.junit.platform.runner)
        }

        // 正确配置KSP处理器
        dependencies {
            add("kspDesktop", project(":desktop-processor"))
        }
    }
}

android {
    namespace = "sophon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "sophon.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateAppInfo)
}

dependencies {
}

compose.desktop {
    application {
        mainClass = "sophon.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Sophon"
            packageVersion = "1.0.1"
            includeAllModules = true

            macOS {
                // 设置图标
                iconFile.set(layout.projectDirectory.dir("src/desktopMain/launcher/icon.icns").asFile)
            }
        }
    }
}
