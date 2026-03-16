package sophon.desktop.feature.i18n.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.i18n.domain.model.I18nConfig
import sophon.desktop.feature.i18n.domain.model.I18nModule
import sophon.desktop.feature.i18n.domain.model.I18nProject
import sophon.desktop.feature.i18n.domain.repository.I18nRepository
import java.io.File

class I18nRepositoryImpl : I18nRepository {

    override suspend fun parseProjectStructure(path: String): I18nProject =
        withContext(Dispatchers.IO) {
            val modules = mutableListOf<I18nModule>()
            buildModulesWithRes(path, 0, modules)
            I18nProject(path, modules)
        }

    override suspend fun findI18nToolPath(): String {
        // Compose Desktop 打包后的资源目录属性
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            // 打包模式：在资源目录下的 tools/i18n
            val deployedI18N = File("/Applications/Sophon.app/Contents/Resources/tools", "i18n")
            if (deployedI18N.exists()) {
                return deployedI18N.absolutePath
            }
        }

        // Debug/开发模式：尝试多个可能的路径
        val candidatePaths = listOf(
            "composeApp/src/desktopMain/tools/i18n",
            "src/desktopMain/tools/i18n",
            "tools/i18n"
        )

        return candidatePaths.firstOrNull { File(it).exists() }
            ?: File("composeApp/src/desktopMain/tools/i18n").absolutePath
    }

    override fun executeTranslation(config: I18nConfig): Flow<String> = flow {
        // 构建命令
        val command = if (config.overrideMode) {
            "${config.toolPath} append --src ${config.csvPath} --out ${config.modulePath}/src/main/res --nolint --prefer-new"
        } else {
            "${config.toolPath} append --src ${config.csvPath} --out ${config.modulePath}/src/main/res --nolint"
        }

        emit("执行命令: $command\n\n")

        try {
            val result = command.simpleShell()
            emit(result)
        } catch (e: Exception) {
            emit("命令执行失败: ${e.message}\n")
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 寻找所有包含res文件夹的Module
     */
    private fun buildModulesWithRes(
        parentPath: String,
        level: Int,
        result: MutableList<I18nModule>,
    ) {
        val parent = File(parentPath)
        val children = parent.listFiles()?.sorted() ?: return
        if (children.isEmpty()) return
        val hit = parent.containsBuildGradle() && parent.containsResDir()
        if (hit) result.add(
            I18nModule(parent.name, level, parent.absolutePath)
        )
        children.forEach {
            buildModulesWithRes(
                it.absolutePath,
                if (hit) level + 1 else level,
                result
            )
        }
    }

    /**
     * 包含build.gradle文件
     */
    private fun File.containsBuildGradle(): Boolean {
        val children = listFiles() ?: return false
        return children.find { it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts") } != null
    }

    /**
     * 包含res文件夹
     */
    private fun File.containsResDir(): Boolean {
        val resFile = File(this.absolutePath, "src/main/res")
        return resFile.exists() && resFile.isDirectory
    }
}
