package sophon.desktop.feature.device

import kotlinx.coroutines.runBlocking
import org.junit.Test
import sophon.desktop.feature.device.data.repository.DeviceInfoRepositoryImpl

class GetDeviceInfoTest {

    @Test
    fun start() {
        runBlocking {
            val repo = DeviceInfoRepositoryImpl()
            val result = repo.getDeviceInfo()
            println(result)
        }
    }

}