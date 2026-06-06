package com.justnothing.methodsclient.highlighter;

import com.justnothing.testmodule.command.output.InputMode;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 高亮模式管理器。
 * <p>
 * 通过 {@link SwitchableHighlighter} 代理实现运行时切换。
 * 模式名称使用 {@link InputMode} 常量定义。
 */
public class HighlighterManager {

    private static LineReader boundReader;
    private static SwitchableHighlighter switchableHighlighter;
    private static String currentMode = InputMode.COMMAND;

    static {
        // 预注册内置高亮器工厂（供 createHighlighter 使用）
    }

    /**
     * 绑定 LineReader 和 SwitchableHighlighter 实例。
     * 必须在 REPL 启动时调用一次，传入构建时创建的代理高亮器。
     */
    public static void setLineReader(LineReader reader, SwitchableHighlighter switchable) {
        boundReader = reader;
        switchableHighlighter = switchable;
    }

    /** 兼容旧接口：只绑定 reader（switchable 由外部管理） */
    public static void setLineReader(LineReader reader) {
        boundReader = reader;
        // 如果 reader 的 highlighter 已经是 Switchable 类型，直接引用
        if (reader != null && reader.getHighlighter() instanceof SwitchableHighlighter sh) {
            switchableHighlighter = sh;
        }
    }

    /**
     * 切换到指定高亮模式。
     *
     * @param modeName 模式名称，应使用 {@link InputMode} 中的常量
     * @return 是否切换成功
     */
    public static boolean switchMode(String modeName) {
        if (modeName == null || modeName.isEmpty()) {
            return false;
        }

        String normalized = modeName.toLowerCase();

        // 验证是否为已知模式
        if (!isKnownMode(normalized)) {
            return false;
        }

        currentMode = normalized;

        // 通过代理切换
        if (switchableHighlighter != null) {
            switchableHighlighter.switchMode(normalized);
        }

        return true;
    }

    /**
     * 注册自定义高亮器工厂（扩展用）
     */
    public static void registerHighlighter(String modeName, Supplier<Highlighter> factory) {
        // 可在 SwitchableHighlighter.createHighlighter 中扩展
    }

    /** 获取当前高亮模式名称 */
    public static String getCurrentMode() {
        return currentMode;
    }

    /** 获取代理高亮器实例（供 ReplClient 构建时使用） */
    public static SwitchableHighlighter createSwitchableHighlighter() {
        return new SwitchableHighlighter();
    }

    private static boolean isKnownMode(String mode) {
        return InputMode.COMMAND.equals(mode)
                || InputMode.JAVA.equals(mode)
                || InputMode.SCRIPT.equals(mode)
                || InputMode.NONE.equals(mode);
    }
}
