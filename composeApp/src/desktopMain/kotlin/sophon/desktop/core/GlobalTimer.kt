package sophon.desktop.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局计时器
 */
object GlobalTimer {

    /**
     * 定时器触发标志
     */
    private val _tick = MutableStateFlow(-1L)
    val tick = _tick.asStateFlow()

    suspend fun start() {
        while (true) {
            _tick.value = System.currentTimeMillis()
            delay(3000)
        }
    }

}