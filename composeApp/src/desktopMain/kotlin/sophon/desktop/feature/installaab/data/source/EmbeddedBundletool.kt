package sophon.desktop.feature.installaab.data.source

import java.io.File

/**
 * 内置 bundletool 工具封装。
 *
 * bundletool.jar 随应用打包在 appResources 中：
 *   - common/tools/bundletool.jar（跨平台 JAR，无需区分操作系统）
 *
 * 打包后 compose.application 会将 common/ 子目录内容平铺到 resources.dir 下，
 * 与 EmbeddedProtoc 保持一致的路径约定。
 */
internal object EmbeddedBundletool {

    val jarPath: String by lazy { resolveJarPath() }

    private fun resolveJarPath(): String {
        // 打包模式：compose.application 将 common/tools/ 平铺到 resources.dir/tools/
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val f = File(resourcesDir, "tools/bundletool.jar")
            if (f.exists()) return f.absolutePath
        }

        // 开发模式：直接读取源目录
        val candidates = listOf(
            "composeApp/src/desktopMain/appResources/common/tools/bundletool.jar",
            "src/desktopMain/appResources/common/tools/bundletool.jar",
        )
        return candidates.firstOrNull { File(it).exists() }
            ?: candidates.first()
    }
}
