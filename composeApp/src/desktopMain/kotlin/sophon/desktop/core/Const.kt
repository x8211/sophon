package sophon.desktop.core

import java.io.File

val PB_HOME = "${System.getProperty("user.home")}/maa_pb"

val SERVER_SRC_DIR = "${System.getProperty("user.dir")}/src/desktopMain/server" //debug模式路径
//const val SERVER_SRC_DIR = "/Applications/Sophon.app/Contents/server" //release模式路径

val SERVER_SRC_PATH
    get() = File(SERVER_SRC_DIR).listFiles { it.absolutePath.endsWith(".dex") }
        .firstOrNull()?.absolutePath

val SERVER_SRC_DEX_NAME
    get() = File(SERVER_SRC_DIR).listFiles { it.absolutePath.endsWith(".dex") }
        .firstOrNull()?.absolutePath?.split("/")?.last()

const val SERVER_DST_DIR = "/data/local/tmp/sophon"
val SERVER_DST_PATH get() = "$SERVER_DST_DIR/$SERVER_SRC_DEX_NAME"
const val SERVER_MAIN_CLASS = "sophon.server.Server"