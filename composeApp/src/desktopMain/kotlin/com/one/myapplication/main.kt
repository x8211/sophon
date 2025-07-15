package com.one.myapplication

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.processor.SlotManger

fun main() = application {
    // 使用生成的SlotManager类
    val slotList = SlotManger.list
    println("Available slots: $slotList")
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "MyApplication",
    ) {
        App()
    }
}