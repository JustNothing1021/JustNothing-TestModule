// rich/src/main/kotlin/com/justnothing/richconsole/widgets/ProgressBar.kt
package com.justnothing.richconsole.widgets

import com.justnothing.richconsole.console.Console

/**
 * 一个简单的进度条，可显示在终端底部状态栏
 * @param console   Console 实例（用于刷新状态栏）
 * @param total     总进度值
 * @param width     进度条宽度（字符数）
 * @param label     左侧标签文字
 */
class ProgressBar(
    private val console: Console,
    private val total: Int,
    private val width: Int = 40,
    private val label: String = "Progress"
) : StatusWidget {

    private var current: Int = 0

    /**
     * 更新当前进度（线程安全）
     * @param value 新的进度值（0 <= value <= total）
     */
    @Synchronized
    fun update(value: Int) {
        current = value.coerceIn(0, total)
        // 每次更新后主动刷新状态栏
        console.refreshStatus()
    }

    /**
     * 渲染进度条的文本表示
     * @return 包含一行进度条字符串的列表
     */
    override fun render(): List<String> {
        val percent = current.toDouble() / total
        val filled = (width * percent).toInt()
        val bar = "▓".repeat(filled) + "░".repeat(width - filled)
        val percentText = "${(percent * 100).toInt()}%"
        val line = "$label: $bar $percentText"
        return listOf(line)
    }
}