package com.justnothing.methodsclient.tui.ansi;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.PrintWriter;

/**
 * ANSI 渲染底层工具类
 * <p>
 * 提供光标控制、行清除、彩色输出等基础能力，
 * 基于 JLine Terminal.writer()，兼容 Android 环境。
 * <p>
 * 核心原理：
 * <ul>
 *   <li>\r          — 回到行首（不换行），用于单行覆写</li>
 *   <li>\033[2K     — 清除从光标到行尾的内容</li>
 *   <li>\033[nA     — 光标上移 n 行</li>
 *   <li>\033[nB     — 光标下移 n 行</li>
 *   <li>\033[?25l   — 隐藏光标</li>
 *   <li>\033[?25h   — 显示光标</li>
 * </ul>
 */
public final class AnsiRenderer {

    private final Terminal terminal;
    private final PrintWriter writer;

    // ANSI 转义序列
    public static final String CR = "\r";
    public static final String CLEAR_LINE = "\033[2K";
    public static final String CLEAR_LINE_LEFT = "\033[1K";
    public static final String HIDE_CURSOR = "\033[?25l";
    public static final String SHOW_CURSOR = "\033[?25h";
    public static final String SAVE_CURSOR = "\033[s";
    public static final String RESTORE_CURSOR = "\033[u";

    /** 上移光标 n 行 */
    public static String cursorUp(int n) {
        return "\033[" + n + "A";
    }

    /** 下移光标 n 行 */
    public static String cursorDown(int n) {
        return "\033[" + n + "B";
    }

    /** 前移光标 n 列 */
    public static String cursorForward(int n) {
        return "\033[" + n + "C";
    }

    /** 后移光标 n 列 */
    public static String cursorBack(int n) {
        return "\033[" + n + "D";
    }

    public AnsiRenderer(Terminal terminal) {
        this.terminal = terminal;
        this.writer = terminal.writer();
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    /** 获取终端宽度（列数） */
    public int getWidth() {
        return terminal.getWidth();
    }

    /** 获取终端高度（行数） */
    public int getHeight() {
        return terminal.getHeight();
    }

    /**
     * 单行覆写：回到行首 → 清除整行 → 写入新内容
     * 用于进度条、转圈等单行动态控件
     */
    public void overwriteLine(String content) {
        writer.print(CR + CLEAR_LINE + content);
        writer.flush();
        System.err.println("[TUI-Render] overwriteLine: len=" + content.length()
            + " preview='" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + "'");
    }

    /**
     * 使用 AttributedString 进行带样式的单行覆写
     */
    public void overwriteLine(AttributedString content) {
        writer.print(CR);
        content.println(terminal);
        writer.flush();
    }

    /**
     * 多行覆写：上移 n 行 → 逐行重写
     * 用于状态面板、日志等多行控件
     *
     * @param lines    新的行内容
     * @param prevLines 之前的行数（用于计算需要上移多少）
     */
    public void overwriteLines(String[] lines, int prevLines) {
        if (prevLines > 0) {
            writer.print(cursorUp(prevLines));
        }
        for (String line : lines) {
            writer.print(CLEAR_LINE + line + "\n");
        }
        writer.flush();
    }

    /**
     * 使用 AttributedString 数组进行多行覆写
     */
    public void overwriteLines(AttributedString[] lines, int prevLines) {
        if (prevLines > 0) {
            writer.print(cursorUp(prevLines));
        }
        for (AttributedString l : lines) {
            writer.print(CLEAR_LINE);
            l.println(terminal);
        }
        writer.flush();
    }

    /** 输出原始字符串并刷新 */
    public void print(String text) {
        writer.print(text);
        writer.flush();
    }

    /** 输出原始字符串 + 换行并刷新 */
    public void println(String text) {
        writer.println(text);
        writer.flush();
    }

    /** 输出 AttributedString 并刷新 */
    public void print(AttributedString text) {
        text.print(terminal);
        writer.flush();
    }

    /** 输出 AttributedString + 换行并刷新 */
    public void println(AttributedString text) {
        text.println(terminal);
        writer.flush();
    }

    /** 隐藏光标 */
    public void hideCursor() {
        writer.print(HIDE_CURSOR);
        writer.flush();
    }

    /** 显示光标 */
    public void showCursor() {
        writer.print(SHOW_CURSOR);
        writer.flush();
    }

    // ========== 颜色/样式快捷方法 ==========

    /** 创建彩色字符串 */
    public static AttributedString colored(String text, int fgColor) {
        return new AttributedString(text, AttributedStyle.DEFAULT.foreground(fgColor));
    }

    /** 创建彩色+粗体字符串 */
    public static AttributedString coloredBold(String text, int fgColor) {
        return new AttributedString(text, AttributedStyle.DEFAULT.foreground(fgColor).bold());
    }

    /** 创建彩色字符串（使用 Colors 枚举值） */
    public static AttributedString colored(String text, byte colorByte) {
        return new AttributedString(text, AttributedStyle.DEFAULT.foreground(ansiColor(colorByte)));
    }

    /**
     * 将项目自定义颜色常量转换为 JLine ANSI 颜色索引
     */
    public static int ansiColor(byte colorByte) {
        return switch (colorByte) {
            case 0 -> AttributedStyle.BLACK;      // DEFAULT/BLACK
            case 1 -> AttributedStyle.RED;
            case 2 -> AttributedStyle.GREEN;
            case 3 -> AttributedStyle.YELLOW;
            case 4 -> AttributedStyle.BLUE;
            case 5 -> AttributedStyle.MAGENTA;
            case 6 -> AttributedStyle.CYAN;
            case 7 -> AttributedStyle.WHITE;
            default -> 0;  // DEFAULT = no color
        };
    }

    /**
     * 将文本截断到指定显示宽度（考虑 CJK 全角字符占 2 列）
     */
    public static String truncateToWidth(String text, int maxWidth) {
        if (text == null) return "";
        int width = displayWidth(text);
        if (width <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        int currentWidth = 0;
        for (int i = 0; i < text.length() && currentWidth < maxWidth; i++) {
            char c = text.charAt(i);
            int charWidth = isFullWidth(c) ? 2 : 1;
            if (currentWidth + charWidth > maxWidth) break;
            sb.append(c);
            currentWidth += charWidth;
        }
        return sb.toString();
    }

    /**
     * 计算字符串的显示宽度（CJK 全角字符算 2 列）
     */
    public static int displayWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += isFullWidth(text.charAt(i)) ? 2 : 1;
        }
        return width;
    }

    /**
     * 判断字符是否为全角字符（CJK 等）
     * 注意: Java char 是 16-bit， supplementary 字符（>\uFFFF）需要用 int codePoint 判断
     */
    public static boolean isFullWidth(char c) {
        // 先用 codePoint 处理（处理代理对情况）
        int cp = c;
        if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
            // 代理对字符在单独判断时无法准确识别，保守返回 true
            return true;
        }
        return isFullWidthCodePoint(cp);
    }

    /** 使用 codePoint 判断是否全宽 */
    public static boolean isFullWidthCodePoint(int cp) {
        return cp >= '\u1100' && (
            cp <= '\u115F' ||       // Hangul Jamo
            cp == '\u2329' || cp == '\u232A' ||  // angle brackets
            (cp >= '\u2E80' && cp <= '\u4DBF') ||  // CJK Radicals / Ideographs A
            (cp >= '\u4E00' && cp <= '\u9FFF') ||  // CJK Unified Ideographs
            (cp >= '\uA000' && cp <= '\uD7FF') ||  // Yi / Hangul
            (cp >= '\uF900' && cp <= '\uFAFF') ||  // CJK Compat Ideographs
            (cp >= '\uFE00' && cp <= '\uFE0F') ||  // Variation Selectors
            (cp >= '\uFF00' && cp <= '\uFF60') ||  // Fullwidth Forms
            (cp >= '\uFFE0' && cp <= '\uFFE6') ||
            (cp >= 0x20000 && cp <= 0x2FFFD)      // CJK Unified Ideographs Ext B
        );
    }

    /**
     * 右侧填充空格到指定宽度
     */
    public static String padRight(String text, int totalWidth) {
        int width = displayWidth(text);
        if (width >= totalWidth) return text;
        StringBuilder sb = new StringBuilder(text);
        for (int i = width; i < totalWidth; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * 左侧填充空格到指定宽度
     */
    public static String padLeft(String text, int totalWidth) {
        int width = displayWidth(text);
        if (width >= totalWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = width; i < totalWidth; i++) {
            sb.append(' ');
        }
        sb.append(text);
        return sb.toString();
    }
}
