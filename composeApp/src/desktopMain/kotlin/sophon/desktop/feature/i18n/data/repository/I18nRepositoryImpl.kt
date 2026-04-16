package sophon.desktop.feature.i18n.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sophon.desktop.core.Shell.simpleShell
import sophon.desktop.feature.i18n.model.I18nConfig
import sophon.desktop.feature.i18n.model.I18nModule
import sophon.desktop.feature.i18n.model.I18nProject
import java.io.File

class I18nRepositoryImpl : I18nRepository {

    override suspend fun parseProjectStructure(path: String): I18nProject =
        withContext(Dispatchers.IO) {
            val modules = mutableListOf<I18nModule>()
            buildModulesWithRes(path, 0, modules)
            I18nProject(path, modules)
        }

    override suspend fun findI18nToolPath(): String {
        val i18nBinary = "i18n"

        // 打包后 appResourcesRootDir 会将 macos/ 子目录的内容
        // 合并到 compose.application.resources.dir 下（平台前缀被去掉）
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val deployedI18N = File(resourcesDir, "tools/$i18nBinary")
            if (deployedI18N.exists()) return deployedI18N.absolutePath
        }

        // 开发模式回退路径
        val candidatePaths = listOf(
            "composeApp/src/desktopMain/appResources/macos/tools/$i18nBinary",
            "src/desktopMain/appResources/macos/tools/$i18nBinary",
        )

        return candidatePaths.firstOrNull { File(it).exists() }
            ?: File(candidatePaths.first()).absolutePath
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
