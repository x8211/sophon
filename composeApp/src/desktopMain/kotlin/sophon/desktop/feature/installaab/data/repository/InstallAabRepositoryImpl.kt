package sophon.desktop.feature.installaab.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import sophon.desktop.core.Context
import sophon.desktop.core.Shell.streamShell
import sophon.desktop.core.datastore.DataStoreProvider
import sophon.desktop.feature.installaab.data.source.AabKeystoreCache
import sophon.desktop.feature.installaab.data.source.EmbeddedBundletool
import sophon.desktop.feature.installaab.model.AabInstallConfig
import java.nio.file.Files

class InstallAabRepositoryImpl : InstallAabRepository {

    override fun getKeystoreCache(): Flow<AabKeystoreCache> =
        DataStoreProvider.aabKeystoreCache.data

    override suspend fun saveKeystoreCache(cache: AabKeystoreCache) {
        DataStoreProvider.aabKeystoreCache.updateData { cache }
    }

    override fun installAab(config: AabInstallConfig): Flow<String> = flow {
        val jarPath = EmbeddedBundletool.jarPath
        // bundletool build-apks 要求输出文件不存在，先占位取名再删除
        val apksOutput = Files.createTempFile("sophon-aab", ".apks").toFile().also { it.delete() }
        val adbState = Context.stream.value
        val adbPath = adbState.adbToolPath
        val serial = adbState.selectedDevice

        try {
            emit("▶ Step 1/2：正在将 AAB 转换为 APK Set...\n")

            val buildCmd = buildBuildApksCmd(jarPath, config, apksOutput.absolutePath, adbPath)
            emitAll(buildCmd.streamShell())

            if (!apksOutput.exists()) {
                emit("\n✗ Step 1 失败，APK Set 未生成，已中止安装\n")
                return@flow
            }

            emit("\n▶ Step 2/2：正在安装到设备...\n")

            val installCmd = buildInstallApksCmd(jarPath, apksOutput.absolutePath, adbPath, serial)
            emitAll(installCmd.streamShell())

            emit("\n✓ 安装完成\n")
        } finally {
            apksOutput.delete()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildBuildApksCmd(
        jarPath: String,
        config: AabInstallConfig,
        apksOutput: String,
        adbPath: String,
    ): String = buildString {
        append("java -jar \"$jarPath\" build-apks")
        append(" --bundle=\"${config.aabPath}\"")
        append(" --output=\"$apksOutput\"")
        append(" --connected-device")
        append(" --adb=\"$adbPath\"")
        append(" --ks=\"${config.keystorePath}\"")
        append(" --ks-pass=pass:${config.storePassword}")
        append(" --ks-key-alias=${config.keyAlias}")
        append(" --key-pass=pass:${config.keyPassword}")
    }

    private fun buildInstallApksCmd(
        jarPath: String,
        apksOutput: String,
        adbPath: String,
        serial: String,
    ): String = buildString {
        append("java -jar \"$jarPath\" install-apks")
        append(" --apks=\"$apksOutput\"")
        append(" --adb=\"$adbPath\"")
        if (serial.isNotBlank()) append(" --device-id=$serial")
    }
}
