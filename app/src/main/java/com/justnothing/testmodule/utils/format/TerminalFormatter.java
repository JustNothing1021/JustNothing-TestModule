package com.justnothing.testmodule.utils.format;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端文本格式化工具类。
 * <p>
 * 核心能力：
 * <ul>
 *   <li><b>CJK 宽度感知</b> — 中日韩文字占 2 个终端列宽，ASCII 占 1 个</li>
 *   <li><b>自动换行</b> — 在指定宽度处智能断行（优先在空格/标点处断开）</li>
 *   <li><b>盒子绘制</b> — 带边框的文本块，支持标题居中、底部署名</li>
 *   <li><b>对齐填充</b> — 居中 / 左对齐 / 右对齐，均考虑 CJK 宽度</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 单个提示卡片
 * String box = TerminalFormatter.box("你知道吗？", "不，你不知道", "---- JustNothing1021", 50);
 * ctx.println(box, Colors.CYAN);
 *
 * // 列表模式：多个卡片
 * List<String> boxes = TerminalFormatter.boxList(tips, 50);
 * for (String b : boxes) { ctx.println(b); }
 * }</pre>
 */
public final class TerminalFormatter {

    private TerminalFormatter() {} // 工具类，禁止实例化

    // ==================== CJK 宽度计算 ====================

    /**
     * 计算字符串的终端显示宽度。
     * <p>
     * 规则：
     * <ul>
     *   <li>ASCII 可打印字符（0x20~0x7E）→ 宽度 1</li>
     *   <li>CJK 统一汉字、日文假名、韩文谚文 → 宽度 2</li>
     *   <li>其他 Unicode 字符（emoji 等）→ 宽度 1（保守估计）</li>
     *   <li>控制字符（\t, \n 等）→ 宽度 0</li>
     * </ul>
     *
     * @param text 输入文本
     * @return 终端显示列宽
     */
    public static int displayWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            width += charDisplayWidth(c);
        }
        return width;
    }

    /**
     * 单字符的终端显示宽度。
     */
    public static int charDisplayWidth(char c) {
        if (c == '\t') return 0;       // Tab 由调用方处理
        if (c == '\n' || c == '\r') return 0;
        if (c < 0x20) return 0;        // 控制字符
        // CJK 范围判断
        if (isCjk(c)) return 2;
        // ASCII 可打印 + 其他 → 1
        return 1;
    }

    /**
     * 判断字符是否属于 CJK 全角范围。
     */
    private static boolean isCjk(char c) {
        // CJK Unified Ideographs
        if (c >= 0x4E00 && c <= 0x9FFF) return true;
        // CJK Extension A
        if (c >= 0x3400 && c <= 0x4DBF) return true;
        // CJK Compatibility Ideographs
        if (c >= 0xF900 && c <= 0xFAFF) return true;
        // Fullwidth forms (！ＡＢ... 全角字母/数字/符号)
        if (c >= 0xFF01 && c <= 0xFF5E) return true;
        // Hangul Syllables
        if (c >= 0xAC00 && c <= 0xD7AF) return true;
        // Hiragana
        if (c >= 0x3040 && c <= 0x309F) return true;
        // Katakana
        if (c >= 0x30A0 && c <= 0x30FF) return true;
        // CJK Punctuation (。，、：；！？""''【】《》…—)
        if (c >= 0x3000 && c <= 0x303F) return true;
        // Halfwidth Hangul
        if (c >= 0xFFA0 && c <= 0xFFDC) return true;
        return false;
    }

    // ==================== 自动换行 ====================

    /**
     * 将文本按指定最大宽度自动分行。
     * <p>
     * 断行策略（按优先级）：
     * <ol>
     *   <li>已有的 {@code \n} 换行符</li>
     *   <li>空格处断开（英文单词边界）</li>
     *   <li>CJK 标点后断开（。，、：；！？）</li>
     *   <li>任意字符处强制断开</li>
     * </ol>
     *
     * @param text     原始文本
     * @param maxWidth 每行最大显示宽度（列数）
     * @return 分行后的字符串列表，每行的 displayWidth ≤ maxWidth
     */
    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        // 先按 \n 分割成段落
        String[] paragraphs = text.split("\n", -1);
        for (String para : paragraphs) {
            wrapParagraph(para, maxWidth, lines);
        }
        return lines;
    }

    private static void wrapParagraph(String text, int maxWidth, List<String> output) {
        if (text.isEmpty()) {
            output.add("");
            return;
        }

        int currentWidth = 0;
        int lineStart = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cw = charDisplayWidth(c);

            if (currentWidth + cw > maxWidth) {
                // 需要断行 —— 尝试回溯找最佳断点
                int breakPoint = findBestBreakPoint(text, lineStart, i, maxWidth);
                if (breakPoint > lineStart) {
                    output.add(text.substring(lineStart, breakPoint));
                    // 跳过断点处的空白
                    lineStart = skipWhitespace(text, breakPoint);
                    i = lineStart - 1; // 循环会 ++i
                } else {
                    // 无法找到好的断点，强制在此处断开
                    output.add(text.substring(lineStart, i));
                    lineStart = i;
                    i--; // 当前字符归入下一行
                }
                currentWidth = 0;
            } else {
                currentWidth += cw;
            }
        }

        // 处理剩余部分
        if (lineStart < text.length()) {
            output.add(text.substring(lineStart));
        }
    }

    /**
     * 从 pos 向前回溯，寻找最佳断行位置。
     * 优先级：空格 > CJK 标点 > 任意位置
     */
    private static int findBestBreakPoint(String text, int start, int end, int maxWidth) {
        // 策略1: 向前找最后一个空格
        for (int i = end - 1; i > start; i--) {
            if (text.charAt(i) == ' ') return i;
        }

        // 策略2: 向前找 CJK 标点（在其后面断开）
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (isCjkPunctuation(c)) return i + 1;
        }

        // 策略3: 在 end 处强制断开
        return end;
    }

    private static boolean isCjkPunctuation(char c) {
        return c == '。' || c == '，' || c == '、' || c == '：'
            || c == '；' || c == '！' || c == '？'
            || c == '”' || c == '\'' || c == '』' || c == '》'
            || c == '…';
    }

    private static int skipWhitespace(String text, int from) {
        int i = from;
        while (i < text.length() && text.charAt(i) == ' ') i++;
        return i;
    }

    // ==================== 对齐与填充 ====================

    /**
     * 将文本居中到指定显示宽度内（CJK 感知）。
     * <p>
     * 如果文本显示宽度已超过目标宽度，原样返回。
     *
     * @param text  文本
     * @param width 目标显示宽度
     * @return 填充后的字符串
     */
    public static String padCenter(String text, int width) {
        int dw = displayWidth(text);
        if (dw >= width) return text;
        int padding = width - dw;
        int leftPad = padding / 2;
        int rightPad = padding - leftPad;
        return spaces(leftPad) + text + spaces(rightPad);
    }

    /**
     * 左侧填充至指定显示宽度（CJK 感知）。
     */
    public static String padLeft(String text, int width) {
        int dw = displayWidth(text);
        if (dw >= width) return text;
        return spaces(width - dw) + text;
    }

    /**
     * 右侧填充至指定显示宽度（CJK 感知）。
     */
    public static String padRight(String text, int width) {
        int dw = displayWidth(text);
        if (dw >= width) return text;
        return text + spaces(width - dw);
    }

    /**
     * 生成 n 个空格字符串。
     */
    public static String spaces(int n) {
        if (n <= 0) return "";
        return " ".repeat(n);
    }

    /**
     * 生成 n 个指定字符的重复字符串。
     */
    public static String repeat(char c, int n) {
        if (n <= 0) return "";
        return String.valueOf(c).repeat(n);
    }

    // ==================== 盒子绘制 ====================

    /** 默认盒子内边距（左右各留空格数） */
    public static final int DEFAULT_PADDING = 1;

    /**
     * 绘制单个带边框的提示卡片。
     * <p>
     * 格式：
     * <pre>
     * ┌──────────────────────────────────────┐
     * │           标题文字（居中）            │
     * ├──────────────────────────────────────┤
     * │                                      │
     * │  内容文字（自动换行，左对齐）         │
     * │                                      │
     * │                        (第N/M条)     │
     * │                  ---- 作者名          │
     * └──────────────────────────────────────┘
     * </pre>
     *
     * @param title      卡片标题（如 "> 你知道吗 <"）
     * @param content    卡片正文内容
     * @param footer     底部署名信息（如 "---- JustNothing1021 [CLI]"）
     * @param boxWidth   盒子总宽度（不含边框），即内容区域的最大列宽
     * @return 完整的多行盒子字符串（含 \n 分隔）
     */
    public static String box(String title, String content, String footer, int boxWidth) {
        return box(title, content, footer, boxWidth, DEFAULT_PADDING);
    }

    /**
     * 绘制带自定义内边距的提示卡片。
     *
     * @param title    卡片标题
     * @param content  卡片正文
     * @param footer   底部署名
     * @param boxWidth 内容区域总宽度
     * @param padding  左右内边距（空格数）
     * @return 完整盒子字符串
     */
    public static String box(String title, String content, String footer,
                             int boxWidth, int padding) {
        StringBuilder sb = new StringBuilder();
        String innerPadding = spaces(padding);
        int innerWidth = boxWidth - padding * 2; // 实际可用内容宽度

        // 顶边框
        sb.append('┌').append(repeat('─', boxWidth)).append("┐\n");

        // 标题行（居中）
        String titleLine = innerPadding + padCenter(title, innerWidth) + innerPadding;
        sb.append('│').append(titleLine).append("│\n");

        // 分隔线
        sb.append('├').append(repeat('─', boxWidth)).append("┤\n");

        // 内容区（自动换行）
        List<String> contentLines = wrapText(content, innerWidth);
        for (String line : contentLines) {
            String padded = innerPadding + padRight(line, innerWidth) + innerPadding;
            sb.append('│').append(padded).append("│\n");
        }

        // 底部署名（右对齐）
        if (footer != null && !footer.isEmpty()) {
            String footerLine = innerPadding + padLeft(footer, innerWidth) + innerPadding;
            sb.append('│').append(footerLine).append("│\n");
        }

        // 底边框
        sb.append('└').append(repeat('─', boxWidth)).append("┘");

        return sb.toString();
    }

    /**
     * 绘制多个盒子的列表形式（用于 --list 和 --search）。
     * <p>
     * 每个条目一个独立卡片，卡片之间用空行分隔。
     *
     * @param entries  条目数据：(title, content, footer) 三元组
     * @param boxWidth 每个卡片的宽度
     * @return 所有卡片的拼接字符串
     */
    public static String boxList(List<BoxEntry> entries, int boxWidth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            BoxEntry entry = entries.get(i);
            sb.append(box(entry.title, entry.content, entry.footer, boxWidth));
            if (i < entries.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 绘制统计信息面板。
     *
     * @param title  面板标题
     * @param rows   键值对行列表 (label, value)
     * @param boxWidth 面板宽度
     * @return 完整面板字符串
     */
    public static String infoPanel(String title, List<InfoRow> rows, int boxWidth) {
        StringBuilder sb = new StringBuilder();
        int padding = DEFAULT_PADDING;
        int innerWidth = boxWidth - padding * 2;
        String innerPad = spaces(padding);

        sb.append('┌').append(repeat('─', boxWidth)).append("┐\n");

        // 标题居中
        sb.append('│').append(innerPad).append(padCenter(title, innerWidth))
                .append(innerPad).append("│\n");

        sb.append('├').append(repeat('─', boxWidth)).append("┤\n");

        for (InfoRow row : rows) {
            String labelPart = row.label;
            String valuePart = padLeft(row.value, innerWidth - displayWidth(labelPart));
            String line = innerPad + labelPart + valuePart + innerPad;
            sb.append('│').append(line).append("│\n");
        }

        sb.append('└').append(repeat('─', boxWidth)).append("┘");
        return sb.toString();
    }

    // ==================== 数据结构 ====================

    /**
     * 盒子条目数据（用于批量绘制）。
     */
    public static class BoxEntry {
        public final String title;
        public final String content;
        public final String footer;

        public BoxEntry(String title, String content, String footer) {
            this.title = title;
            this.content = content;
            this.footer = footer;
        }
    }

    /**
     * 信息面板行数据。
     */
    public static class InfoRow {
        public final String label;
        public final String value;

        public InfoRow(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
