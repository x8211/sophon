package sophon.desktop.feature.apps

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: String,
    val path: String,
    val icon: ImageBitmap?,
    val minSdkVersion: String = "",
    val targetSdkVersion: String = "",
    val compileSdkVersion: String = "",
    val buildToolsVersion: String = "",
    val dependencies: List<String> = emptyList(),
    val size: Long = 0,
    val dataSize: Long = 0,
    val cacheSize: Long = 0,
)