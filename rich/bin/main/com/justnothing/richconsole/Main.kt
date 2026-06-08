@file:JvmName("Main")
package com.justnothing.richconsole

import com.justnothing.richconsole.widgets.ProgressBar
import com.justnothing.richconsole.console.Console
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

fun main(): Unit = runBlocking {
    val rootLogger = Logger.getLogger("")
    rootLogger.level = Level.FINEST
    for (handler in rootLogger.handlers) {
        handler.level = Level.FINEST
    }
    Logger.getLogger("org.jline").level = Level.FINEST

    val console = Console.create()

    // 1. 先显示一个欢迎信息（在上方输出区）
    console.printlnAbove("=== Rich Console Demo ===")
    console.printlnAbove("下方会显示一个进度条，并且你可以随时输入命令。")

    // 2. 创建一个进度条组件并设置为状态栏
    val progress = ProgressBar(console, total = 100, width = 50, label = "Loading")
    console.setStatusWidget(progress)

    // 3. 模拟一个后台任务，每隔 100ms 更新进度
    val job = launch {
        for (i in 0..100) {
            progress.update(i)
            delay(100)
        }
        console.printlnAbove("✅ 任务完成！")
    }

    // 4. 主线程处理用户输入（不会阻塞进度条更新）
    while (true) {
        val input = console.readLine("> ") ?: break   // Ctrl+D 退出
        when (input.trim().lowercase()) {
            "exit", "quit" -> {
                console.printlnAbove("👋 再见！")
                break
            }
            "status" -> {
                console.printlnAbove("当前进度条状态：请观察底部状态栏")
            }
            else -> {
                console.printlnAbove("你输入了: $input")
            }
        }
    }

    job.join()
}