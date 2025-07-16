package sophon.server

import android.util.Log

const val TAG = "Sophon"

fun String.logI() {
    Log.i(TAG, "[${Thread.currentThread().name}]$this")
}

fun String.logD() {
    Log.d(TAG, "[${Thread.currentThread().name}]$this")
}

fun String.logV() {
    Log.v(TAG, "[${Thread.currentThread().name}]$this")
}

fun String.logE(tr: Throwable? = null) {
    Log.e(TAG, "[${Thread.currentThread().name}]$this", tr)
}