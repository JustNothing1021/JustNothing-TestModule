// rich/src/main/kotlin/com/justnothing/richconsole/Console.kt
package com.justnothing.richconsole.console

import com.justnothing.richconsole.widgets.StatusWidget
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.PrintAboveWriter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.Status


/**
 * 终端控制台封装：支持上方动态输出 + 底部固定状态栏 + 底部固定输入行
 * @param terminal JLine 终端实例
 * @param reader   行读取器（提供输入历史、自动补全等）
 * @param aboveWriter 专门用于在上方输出内容的 Writer（不会干扰输入行）
 */
class Console private constructor(
    private val terminal: Terminal,
    private val reader: LineReader,
    private val aboveWriter: PrintAboveWriter
) {
    // JLine 内置的状态栏管理器，用于在输入行正上方显示固定内容
    private val statusBar: Status = Status.getStatus(terminal, true)

    // 当前激活的状态栏组件（只能显示一个，但组件内部可组合多个）
    private var currentWidget: StatusWidget? = null

    /**
     * 在输入行上方输出普通文本（相当于日志，会向上滚动）
     */
    @Synchronized
    fun printAbove(message: String) {
        aboveWriter.append(message)
        aboveWriter.flush()
    }

    @Synchronized
    fun printlnAbove(message: String) {
        aboveWriter.appendLine(message)
        aboveWriter.flush()
    }

    /**
     * 读取一行用户输入（输入行会固定在屏幕最底部）
     * @param prompt 提示符，例如 "> "
     * @return 用户输入的字符串，如果遇到 EOF 则返回 null
     */
    @Synchronized
    fun readLine(prompt: String): String? {
        return reader.readLine(prompt)
    }

    /**
     * 设置要在底部状态栏显示的组件
     * @param widget 实现了 StatusWidget 接口的组件（如进度条）
     */
    @Synchronized
    fun setStatusWidget(widget: StatusWidget) {
        currentWidget = widget
        refreshStatus()
    }

    /**
     * 手动刷新底部状态栏（会重新调用当前 widget 的 render() 方法并更新显示）
     */
    @Synchronized
    fun refreshStatus() {
        val lines = currentWidget?.render() ?: emptyList()
        // Status.update() 需要 List<AttributedString>，我们简单包装一下
        val attributedLines = lines.map { AttributedString(it) }
        statusBar.update(attributedLines)
    }

    /**
     * 工厂方法：创建 Console 实例（推荐使用）
     */
    companion object {
        @JvmStatic
        fun create(): Console {
            val terminal = TerminalBuilder.builder()
                .jni(true)
                .dumb(false)
                .build()
            val reader = LineReaderBuilder.builder()
                  .terminal(terminal)
                  .build()
            val aboveWriter = PrintAboveWriter(reader)
            return Console(terminal, reader, aboveWriter)
        }
    }
}

