package sophon.server

import android.annotation.SuppressLint
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.util.Log
import java.lang.reflect.Method

@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
object ServiceManager {

    private lateinit var GET_SERVICE_METHOD: Method

    private val services = hashMapOf<String, IInterface?>()

    /**
     * PackageManagerService
     */
    val PMS by lazy { getService("package", "android.content.pm.IPackageManager") }
    
    /**
     * ActivityManagerService
     * 兼容不同Android版本
     */
    val AMS by lazy { getActivityManagerService() }

    init {
        runCatching {
            GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager")
                .getDeclaredMethod("getService", String::class.java)
                .also { it.isAccessible = true }
        }
    }

    private fun getService(serviceName: String, serviceType: String): IInterface? {
        return services.getOrPut("${serviceName}_${serviceType}") {
            runCatching {
                val binder = GET_SERVICE_METHOD.invoke(null, serviceName) as? IBinder
                if (binder == null) throw RuntimeException("cant get binder")

                val stubClass = Class.forName("${serviceType}\$Stub")
                Log.i(TAG, "getService($serviceName), stubClass: $stubClass")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                    .also { it.isAccessible = true }
                if (asInterfaceMethod == null) throw RuntimeException("cant get asInterfaceMethod")

                val iInterface = asInterfaceMethod.invoke(null, binder) as? IInterface
                if (iInterface == null) throw RuntimeException("cant cast to IInterface")

                iInterface
            }.onFailure {
                Log.e(TAG, "getService($serviceName) fail: $serviceName", it)
            }.getOrNull()
        }
    }
    
    /**
     * 获取ActivityManagerService，兼容不同Android版本
     * Android 8.0及以上版本使用IActivityManager
     * Android 8.0以下版本使用ActivityManagerNative
     */
    private fun getActivityManagerService(): Any? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0及以上版本
                val getServiceMethod = Class.forName("android.app.ActivityManager")
                    .getDeclaredMethod("getService")
                    .also { it.isAccessible = true }
                getServiceMethod.invoke(null)
            } else {
                // Android 8.0以下版本
                val getDefaultMethod = Class.forName("android.app.ActivityManagerNative")
                    .getDeclaredMethod("getDefault")
                    .also { it.isAccessible = true }
                getDefaultMethod.invoke(null)
            }
        }.onFailure {
            Log.e(TAG, "getActivityManagerService fail", it)
        }.getOrNull()
    }

}