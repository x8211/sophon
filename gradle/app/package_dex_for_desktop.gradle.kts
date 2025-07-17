// 添加打包Android dex文件并复制到desktopMain/server文件夹的任务
abstract class PackageAndroidDexTask @Inject constructor(
    @get:Internal
    val fileSystemOperations: FileSystemOperations
) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dexDir: DirectoryProperty

    @get:OutputDirectory
    abstract val serverDir: DirectoryProperty


    @TaskAction
    fun execute() {
        // 确保目标目录存在
        serverDir.get().asFile.mkdirs()

        // 复制所有dex文件到目标目录，不保留中间目录结构
        fileSystemOperations.copy {
            from(dexDir)
            include("**/*.dex")
            into(serverDir)
            // 扁平化目录结构，所有dex文件直接放在目标目录下
            eachFile {
                // 只保留文件名，去掉路径
                path = name
            }
            // 不包含空目录
            includeEmptyDirs = false
        }

        // 新增时间戳重命名逻辑
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
        serverDir.get().asFile.listFiles { file ->
            file.isFile && file.extension == "dex"
        }.forEach { dexFile ->
            val newName = "${dexFile.nameWithoutExtension}_$timestamp.dex"
            dexFile.renameTo(File(dexFile.parentFile, newName))
        }

        println("已成功将dex文件复制到 ${serverDir.get()}")
    }
}

tasks.register<PackageAndroidDexTask>("packageAndroidDexForDesktop") {
    description = "打包Android release dex文件并复制到desktopMain/server文件夹"
    group = "sophon"

    // 依赖Android release构建任务
    dependsOn("assembleRelease")

    // 设置输入目录
    dexDir.set(layout.buildDirectory.dir("intermediates/dex/release"))

    // 设置输出目录
    serverDir.set(layout.projectDirectory.dir("src/desktopMain/server"))
}
