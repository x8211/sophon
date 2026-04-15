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
    alias(libs.plugins.serialization)
}

apply {
    from(file(rootProject.layout.projectDirectory.dir("gradle/desktop/copy_distribution_tools.gradle.kts")))
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
            implementation(libs.compose.components.resources)
            implementation(projects.shared)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqlite.jdbc)
        }
        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.engine)
            implementation(libs.junit.platform.runner)
        }
    }
}

android {
    namespace = "sophon.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "sophon.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
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
            // 启用代码压缩和混淆
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

        // Release 包配置（packageReleaseDmg 会使用此配置）
        buildTypes.release.proguard {
            // 启用 ProGuard 处理（minification + obfuscation）
            isEnabled = true
            // 开启混淆（默认 false）
            obfuscate.set(true)
            // 启用优化（默认 true，显式声明）
            optimize.set(true)
            // 指定自定义混淆规则文件
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "Sophon"
            packageVersion = "1.0.0"
            includeAllModules = true

            // appResourcesRootDir 下的 common/、windows/、macOS/、linux/ 子目录
            // 会分别在各平台打包时自动合并进安装包（可通过 compose.application.resources.dir 访问）
            appResourcesRootDir.set(layout.projectDirectory.dir("src/desktopMain/appResources"))

            windows {
                // Windows 安装包图标（优先 .ico，当前使用 .png 由 jpackage 自动转换）
                iconFile.set(layout.projectDirectory.dir("src/desktopMain/launcher/icon.png").asFile)
                // 安装目录名称
                dirChooser = true
                // 每用户安装（无需管理员权限）
                perUserInstall = true
                // 开始菜单快捷方式分组名
                menuGroup = "Sophon"
                // 桌面快捷方式
                shortcut = true
                // MSI/EXE 升级 UUID，固定后可无缝升级，请勿修改
                upgradeUuid = "3B3E2B2A-1C4D-4E5F-8A9B-0C1D2E3F4A5B"
            }

            macOS {
                // App Bundle 唯一标识（反向域名格式，需与 Apple Developer 后台一致）
                bundleID = "com.sophon.desktop"
                // Dock/菜单栏显示名称
                dockName = "Sophon"
                // 设置图标
                iconFile.set(layout.projectDirectory.dir("src/desktopMain/launcher/icon.icns").asFile)

                // ----------------------------------------------------------------
                // macOS 签名配置（通过 Gradle properties 传入，CI/本地均可复用）
                // 使用方式（任选其一）:
                //   1. 在 ~/.gradle/gradle.properties 中配置以下属性
                //   2. 在命令行传入: ./gradlew packageReleaseDmg -Pcompose.desktop.mac.sign=true ...
                //
                // 必填属性:
                //   compose.desktop.mac.sign=true
                //   compose.desktop.mac.signing.identity=Developer ID Application: Your Name (TEAM_ID)
                //
                // 公证（notarize）所需额外属性:
                //   compose.desktop.mac.notarization.appleID=your@apple.id
                //   compose.desktop.mac.notarization.password=@keychain:AC_PASSWORD
                //   compose.desktop.mac.notarization.teamID=YOUR_TEAM_ID
                // ----------------------------------------------------------------
                signing {
                    sign.set(providers.gradleProperty("compose.desktop.mac.sign").map { it.toBoolean() }.orElse(false))
                    identity.set(providers.gradleProperty("compose.desktop.mac.signing.identity").orElse(""))
                }

                notarization {
                    appleID.set(providers.gradleProperty("compose.desktop.mac.notarization.appleID").orElse(""))
                    password.set(providers.gradleProperty("compose.desktop.mac.notarization.password").orElse(""))
                    teamID.set(providers.gradleProperty("compose.desktop.mac.notarization.teamID").orElse(""))
                }
            }
        }
    }
}
