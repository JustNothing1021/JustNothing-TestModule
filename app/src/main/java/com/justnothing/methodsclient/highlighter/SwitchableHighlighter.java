package com.justnothing.methodsclient.highlighter;

import com.justnothing.testmodule.command.output.InputMode;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

import java.util.function.Supplier;

/**
 * 可切换的代理高亮器。
 * <p>
 * JLine 的 {@link LineReader} 不支持运行时替换 Highlighter（只能在构建时通过
 * {@code LineReaderBuilder.highlighter()} 设置）。此类通过代理模式绕过此限制：
 * 构建时设置此代理，运行时切换其内部的委托高亮器即可。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 构建时
 * SwitchableHighlighter proxy = new SwitchableHighlighter();
 * LineReader reader = LineReaderBuilder.builder().highlighter(proxy).build();
 *
 * // 运行时切换（无需重建 reader）
 * proxy.switchTo(InputMode.JAVA);    // → Java 语法高亮
 * proxy.switchTo(InputMode.COMMAND); // → 命令高亮
 * }</pre>
 */
public class SwitchableHighlighter implements Highlighter {

    private volatile Highlighter delegate;
    private String currentMode = InputMode.COMMAND;

    public SwitchableHighlighter() {
        this.delegate = new CommandAwareHighlighter(); // 默认命令模式
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        Highlighter d = delegate;
        return d != null ? d.highlight(reader, buffer) : new AttributedString(buffer);
    }

    /**
     * 切换到指定模式的高亮器
     */
    public void switchMode(String mode) {
        currentMode = mode != null ? mode.toLowerCase() : InputMode.NONE;
        delegate = createHighlighter(currentMode);
    }

    /** 获取当前模式 */
    public String getCurrentMode() {
        return currentMode;
    }

    private static Highlighter createHighlighter(String mode) {
        if (InputMode.JAVA.equals(mode)) {
            return new JavaSyntaxHighlighter();
        } else if (InputMode.COMMAND.equals(mode)) {
            return new CommandAwareHighlighter();
        } else if (InputMode.SCRIPT.equals(mode)) {
            return new JavaSyntaxHighlighter(); // 脚本暂复用Java高亮
        } else {
            // InputMode.NONE or unknown → 关闭高亮
            return null;
        }
    }
}
