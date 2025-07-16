package sophon.desktop.core

val PB_HOME = "${System.getProperty("user.home")}/maa_pb"

//const val SERVER_SRC_PATH = "/Applications/Sophon.app/Contents/server/classes.dex"
val SERVER_SRC_PATH = "${System.getProperty("user.dir")}/src/desktopMain/server/classes.dex"
const val SERVER_DST_DIR = "/data/local/tmp/sophon"
const val SERVER_DST_PATH = "$SERVER_DST_DIR/classes.dex"
const val SERVER_MAIN_CLASS = "sophon.server.Server"