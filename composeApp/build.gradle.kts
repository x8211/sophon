import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        val desktopMain by getting
        val desktopTest by getting // 添加测试源集

        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(libs.serialization.json)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(project(":desktop-processor"))
            implementation(libs.datastore)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.bottom.sheet.nav)
            implementation(libs.voyager.tab.nav)
            implementation(libs.voyager.transitions)
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

dependencies {
}

compose.desktop {
    application {
        mainClass = "sophon.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Sophon"
            packageVersion = "1.0.0"
            includeAllModules = true

            macOS {
                // 设置图标
                iconFile.set(layout.projectDirectory.dir("src/desktopMain/launcher/icon.icns").asFile)
            }
        }
    }
}