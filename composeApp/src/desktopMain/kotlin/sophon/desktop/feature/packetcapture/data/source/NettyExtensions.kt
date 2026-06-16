package sophon.desktop.feature.packetcapture.data.source

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.util.concurrent.Future
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 将 Netty 连接类 [ChannelFuture]（connect/bind）桥接为 suspend 函数。
 * 成功时返回已建立连接的 [Channel]，失败时抛出异常。
 * 协程被取消时会关闭底层 Channel 以释放资源。
 */
internal suspend fun ChannelFuture.awaitChannel(): Channel = suspendCancellableCoroutine { cont ->
    addListener(ChannelFutureListener { cf ->
        if (cf.isSuccess) cont.resume(cf.channel())
        else cont.resumeWithException(cf.cause() ?: RuntimeException("Connection failed"))
    })
    cont.invokeOnCancellation { channel().close() }
}

/**
 * 将 Netty 写入类 [ChannelFuture]（writeAndFlush）桥接为 suspend 函数。
 *
 * 成功时先在 **EventLoop 线程**上同步执行 [onSuccess]，再恢复协程；失败时抛出异常。
 *
 * 与 [Future.awaitHandshake] 的设计动机相同：[onSuccess] 在 EventLoop 线程上原子执行，
 * 确保写入完成后的管道操作（如清理旧 handler、添加新 handler）与随后到来的入站事件
 * 之间不存在竞争窗口。
 */
internal suspend fun ChannelFuture.awaitWrite(
    onSuccess: () -> Unit = {}
): Unit = suspendCancellableCoroutine { cont ->
    addListener(ChannelFutureListener { cf ->
        if (cf.isSuccess) {
            try {
                onSuccess()
                cont.resume(Unit)
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        } else {
            cont.resumeWithException(cf.cause() ?: RuntimeException("Write failed"))
        }
    })
}

/**
 * 将 Netty TLS 握手 [Future]（来自 [io.netty.handler.ssl.SslHandler.handshakeFuture]）
 * 桥接为 suspend 函数。
 *
 * 成功时先在 **EventLoop 线程**上同步执行 [onSuccess]，再恢复协程；失败时抛出异常。
 *
 * [onSuccess] 运行在 EventLoop 线程上，与协程恢复（切换到 IO 线程池）之间**无竞争窗口**，
 * 专用于需要在握手完成后立即操作管道的场景——例如后端 TLS 握手完成后必须在服务端
 * 首个 SETTINGS 帧到达前装配 HTTP/2 codec，否则会触发
 * "First received frame was not SETTINGS" 错误。
 */
internal suspend fun Future<Channel>.awaitHandshake(
    onSuccess: () -> Unit = {}
): Unit = suspendCancellableCoroutine { cont ->
    addListener { f ->
        if (f.isSuccess) {
            try {
                onSuccess()         // EventLoop 线程同步执行，协程尚未恢复
                cont.resume(Unit)
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        } else {
            cont.resumeWithException(f.cause() ?: RuntimeException("TLS handshake failed"))
        }
    }
}
