package sophon.desktop.core

import sophon.desktop.generated.AppInfo

const val APP_NAME = AppInfo.APP_NAME
const val APP_BUNDLE_PATH = "/Applications/$APP_NAME.app/Contents/Resources"

val CACHE_HOME = "${System.getProperty("user.home")}/.${APP_NAME}"
