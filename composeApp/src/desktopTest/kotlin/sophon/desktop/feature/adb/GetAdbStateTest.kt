package sophon.desktop.feature.adb

import kotlinx.coroutines.runBlocking
import org.junit.Test
import sophon.desktop.feature.adb.data.repository.AdbRepositoryImpl
import sophon.desktop.feature.adb.domain.usecase.GetAdbStateUseCase

class GetAdbStateTest {

    @Test
    fun start() {
        runBlocking {
            val useCase = GetAdbStateUseCase(AdbRepositoryImpl(this))
            useCase().collect {
                println(it)
            }
        }
    }

}