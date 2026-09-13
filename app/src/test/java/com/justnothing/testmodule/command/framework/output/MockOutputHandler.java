package com.justnothing.testmodule.command.framework.output;

import com.justnothing.richconsole.console.Console;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试用 MockOutputHandler，将输出捕获到内存中。
 * <p>
 * 用于 {@link ICommandOutputHandler} 的单元测试，支持 CLI 和 JSON 模式验证。
 * </p>
 */
public class MockOutputHandler implements ICommandOutputHandler {

    private final StringBuilder output = new StringBuilder();
    private final List<String> lines = new ArrayList<>();
    private boolean closed = false;

    @Override
    public void print(String text) {
        if (!closed) {
            output.append(text);
        }
    }

    @Override
    public void println(String text) {
        if (!closed) {
            output.append(text).append('\n');
            lines.add(text);
        }
    }

    @Override
    public void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    @Override
    public void printStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        print(sw.toString());
    }

    @Override
    public void printError(String text) {
        print("[ERROR] " + text);
    }

    @Override
    public void printlnError(String text) {
        println("[ERROR] " + text);
    }

    @Override
    public void flush() {
        // 内存输出，无需 flush
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public String getString() {
        return output.toString();
    }

    @Override
    public void clear() {
        reset();
    }

    // ==================== 断言方法 ====================

    /** 获取所有输出文本 */
    public String getOutput() {
        return output.toString();
    }

    /** 获取所有行 */
    public List<String> getLines() {
        return lines;
    }

    /** 输出是否为空 */
    public boolean isEmpty() {
        return output.length() == 0;
    }

    /** 是否包含指定文本 */
    public boolean contains(String text) {
        return output.indexOf(text) >= 0;
    }

    /** 重置输出 */
    public void reset() {
        output.setLength(0);
        lines.clear();
        closed = false;
    }

    /** 是否已关闭 */
    public boolean isClosed() {
        return closed;
    }
}