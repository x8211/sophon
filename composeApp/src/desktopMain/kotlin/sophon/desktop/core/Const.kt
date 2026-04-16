package sophon.desktop.core

import sophon.desktop.generated.AppInfo

const val APP_NAME = AppInfo.APP_NAME

val CACHE_HOME = "${System.getProperty("user.home")}/.${APP_NAME}"
