package sophon.desktop.feature.device

import kotlinx.coroutines.runBlocking
import org.junit.Test
import sophon.desktop.feature.device.data.repository.DeviceInfoRepositoryImpl
import sophon.desktop.feature.device.domain.usecase.GetDeviceInfoUseCase

class GetDeviceInfoTest {

    @Test
    fun start() {
        runBlocking {
            val useCase = GetDeviceInfoUseCase(DeviceInfoRepositoryImpl())
            val result = useCase()
            println(result)
        }
    }

}