package sophon.server.feature

import android.content.pm.PackageInfo
import sophon.server.logI

object PackageManager : Service("package", "android.content.pm.IPackageManager") {

    private val getPackageInfoMethod = manager?.javaClass?.getMethod(
        "getPackageInfo",
        String::class.java, Integer.TYPE, Integer.TYPE
    )?.also { it.isAccessible = true }

    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? {
        "getPackageInfo: $packageName".logI()
        return getPackageInfoMethod?.invoke(manager, packageName, flags, 0) as? PackageInfo
    }

}