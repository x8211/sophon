import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

val appName: String by project
val appVersion: String by project

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

val generateAppInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/appInfo/kotlin")
    val buildTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    val localAppName = appName
    val localAppVersion = appVersion
    inputs.property("version", localAppVersion)
    inputs.property("buildTime", buildTimeStr)
    inputs.property("appName", localAppName)
    outputs.dir(outputDir)

    doLast {
        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val outputFile = outputDir.get().file("sophon/desktop/generated/AppInfo.kt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package sophon.desktop.generated

            object AppInfo {
                const val APP_NAME = "$localAppName"
                const val APP_VERSION = "$localAppVersion"
                const val BUILD_TIME = "$buildTime"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir(generateAppInfo.map { it.outputs.files.asPath })
        }
        val desktopTest by getting

        commonMain.dependencies {
            implementation(compose.components.resources)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.serialization.json)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.netty.all)
            implementation(libs.bouncycastle)
            implementation(libs.protobuf.java)
            implementation(libs.protobuf.java.util)
        }
        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.engine)
            implementation(libs.junit.platform.runner)
        }
    }
}

// 移除 ProGuard 处理后各依赖 JAR 中残留的签名文件，防止运行时 JAR 签名验证失败
// 背景：部分第三方 JAR（如 BouncyCastle）由官方签名。ProGuard 修改字节码后
// SHA-256 哈希不再匹配，但签名文件（.SF/.DSA/.RSA）默认被原样复制到输出 JAR，
// Java ClassLoader 见到签名文件就触发验证 → "SHA-256 digest error"。
val stripJarSignatures by tasks.registering {
    val proguardOutputDir = layout.buildDirectory.dir("compose/tmp/main-release/proguard")
    inputs.dir(proguardOutputDir)
    outputs.dir(proguardOutputDir)

    doLast {
        val jarSignaturePattern = Regex("META-INF/[^/]+\\.(SF|DSA|RSA|EC)$")
        proguardOutputDir.get().asFileTree.filter { it.extension == "jar" }.forEach { jar ->
            val entries = mutableListOf<Pair<String, ByteArray>>()
            var hasSignatureFiles = false
            JarFile(jar, false).use { jarFile ->
                jarFile.entries().asSequence().forEach { entry ->
                    if (jarSignaturePattern.containsMatchIn(entry.name)) {
                        hasSignatureFiles = true
                    } else {
                        entries += entry.name to jarFile.getInputStream(entry).readBytes()
                    }
                }
            }
            if (hasSignatureFiles) {
                val tmp = File(jar.parent, "${jar.name}.tmp")
                JarOutputStream(tmp.outputStream()).use { out ->
                    entries.forEach { (name, bytes) ->
                        out.putNextEntry(JarEntry(name))
                        out.write(bytes)
                        out.closeEntry()
                    }
                }
                jar.delete()
                tmp.renameTo(jar)
                logger.lifecycle("Stripped JAR signatures from ${jar.name}")
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateAppInfo)
}

afterEvaluate {
    // 将签名文件清理任务插入 ProGuard 之后、所有打包任务之前
    val proguardTask = tasks.findByName("proguardReleaseJars")
    if (proguardTask != null) {
        stripJarSignatures.get().dependsOn(proguardTask)
        tasks.matching {
            it.name.startsWith("package") ||
            it.name.startsWith("create") && it.name.contains("Distributable") ||
            it.name.startsWith("create") && it.name.contains("Distribution")
        }.configureEach { dependsOn(stripJarSignatures) }
    }
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
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = appName
            packageVersion = appVersion
            includeAllModules = true

            // appResourcesRootDir 下的 common/、windows/、macOS/、linux/ 子目录
            // 会分别在各平台打包时自动合并进安装包（可通过 compose.application.resources.dir 访问）
            appResourcesRootDir.set(layout.projectDirectory.dir("src/desktopMain/appResources"))

            windows {
                // Windows 安装包图标（优先 .ico）
                iconFile.set(layout.projectDirectory.dir("src/desktopMain/launcher/icon.ico").asFile)
                // 安装目录名称
                dirChooser = true
                // 每用户安装（无需管理员权限）
                perUserInstall = true
                // 开始菜单快捷方式分组名
                menuGroup = appName
                // 桌面快捷方式
                shortcut = true
                // MSI/EXE 升级 UUID，固定后可无缝升级，请勿修改
                upgradeUuid = "3B3E2B2A-1C4D-4E5F-8A9B-0C1D2E3F4A5B"
            }

            macOS {
                bundleID = "sophon.desktop"
                dockName = appName
                // 设置图标
                iconFile.set(layout.projectDirectory.dir("src/desktopMain/launcher/icon.icns").asFile)
            }
        }
    }
}
