package sophon.server

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.IInterface
import sophon.server.feature.Service
import java.lang.reflect.Method

@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
object ServiceManager {

    private var GET_SERVICE_METHOD: Method? = null

    private val services = hashMapOf<Service, IInterface?>()

    init {
        runCatching {
            GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager")
                .getDeclaredMethod("getService", String::class.java)
        }
    }

    fun getService(service: Service): IInterface? {
        return services.getOrPut(service) {
            runCatching {
                val binder =
                    GET_SERVICE_METHOD?.invoke(null, service.name) as? IBinder ?: return null
                val asInterfaceMethod = Class.forName("${service.type}\$Stub")
                    .getMethod("asInterface", IBinder::class.java)
                asInterfaceMethod.isAccessible = true
                asInterfaceMethod.invoke(null, binder) as? IInterface
            }.getOrNull()
        }
    }

}