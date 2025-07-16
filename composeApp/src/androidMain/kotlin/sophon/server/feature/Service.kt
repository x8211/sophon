package sophon.server.feature

import sophon.server.ServiceManager

abstract class Service( val name: String,  val type: String){
    protected val manager = ServiceManager.getService(this)
}