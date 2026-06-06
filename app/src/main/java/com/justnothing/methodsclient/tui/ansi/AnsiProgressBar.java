package com.justnothing.methodsclient.tui.ansi;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * ANSI 进度条控件（全功能可配置版）
 * <p>
 * 单行动态渲染，通过 \r 回车 + 清行 实现原地刷新。
 * 所有显示元素均可独立开关和配置。
 * <p>
 * 使用示例：
 * <pre>{@code
 *   // 简单用法
 *   var bar = new AnsiProgressBar(terminal, "Downloading", 100);
 *   bar.start();
 *   for (int i = 0; i <= 100; i++) { bar.update(i); Thread.sleep(50); }
 *   bar.complete();
 *
 *   // Builder 用法（自定义所有选项）
 *   var bar = AnsiProgressBar.builder(terminal, "Scanning", 200)
 *       .style(AnsiProgressBar.STYLE_UNICODE)
 *       .showPercent(true).showCount(true).showEta(true).showSpeed(true)
 *       .colorGradient(AttributedStyle.RED, AttributedStyle.YELLOW, AttributedStyle.GREEN)
 *       .build();
 * }</pre>
 */
public class AnsiProgressBar implements AnsiWidgetEngine.AnsiWidget {

    // ==================== 预设样式 ====================

    public static final Style STYLE_UNICODE = Style.builder()
        .fill('█').empty('░')
        .leftBracket('[').rightBracket(']')
        .fillColor(AttributedStyle.CYAN).emptyColor(AttributedStyle.BLUE)
        .build();

    public static final Style STYLE_ASCII = Style.builder()
        .fill('#').empty(' ')
        .leftBracket('[').rightBracket(']')
        .fillColor(AttributedStyle.GREEN).emptyColor(0)
        .build();
    public static final Style STYLE_BLOCK = Style.builder()
        .fill('■').empty('□')
        .leftBracket('|').rightBracket('|')
        .fillColor(AttributedStyle.YELLOW).emptyColor(0)
        .build();
    public static final Style STYLE_COLORFUL = Style.builder()
        .fill('\u2588').empty('\u2591')
        .leftBracket('\u250C').rightBracket('\u2510')  // ┌ ┐
        .fillColor(AttributedStyle.CYAN).emptyColor(0)
        .build();

    // ==================== 样式配置 ====================

    /**
     * 进度条视觉样式（字符 + 颜色）
     *
     * @param fillChar     填充字符
     * @param emptyChar    空白字符
     * @param leftBracket  左边框
     * @param rightBracket 右边框
     * @param fillColor    填充颜色（基础色，colorGradient 启用时会被覆盖）
     * @param emptyColor   空白颜色
     */
        public record Style(char fillChar, char emptyChar, char leftBracket, char rightBracket,
                            int fillColor, int emptyColor) {

        public static Builder builder() {
            return new Builder();
        }

            public static class Builder {
                private char fill = '\u2588';
                private char empty = '\u2591';
                private char left = '[';
                private char right = ']';
                private int fillClr = AttributedStyle.CYAN;
                private int emptyClr = 0;

                public Builder fill(char c) {
                    fill = c;
                    return this;
                }

                public Builder empty(char c) {
                    empty = c;
                    return this;
                }

                public Builder leftBracket(char c) {
                    left = c;
                    return this;
                }

                public Builder rightBracket(char c) {
                    right = c;
                    return this;
                }

                public Builder fillColor(int c) {
                    fillClr = c;
                    return this;
                }

                public Builder emptyColor(int c) {
                    emptyClr = c;
                    return this;
                }

                public Style build() {
                    return new Style(fill, empty, left, right, fillClr, emptyClr);
                }
            }
        }

    /**
     * 颜色渐变策略：根据进度百分比动态改变填充颜色
     * <p>
     * 例如：RED(0%) → YELLOW(50%) → GREEN(100%)
     */
    public interface ColorGradient {
        /** 根据百分比(0-100)返回当前颜色 */
        int getColor(int percent);
    }

    /** 无渐变：始终使用 style 中定义的 fillColor */
    public static final ColorGradient GRADIENT_NONE = pct -> -1;  // -1 表示使用默认色

    /** 经典红黄绿渐变（危险→警告→安全） */
    public static final ColorGradient GRADIENT_RYG = new ColorGradient() {
        @Override
        public int getColor(int percent) {
            if (percent < 50) return AttributedStyle.RED;
            if (percent < 80) return AttributedStyle.YELLOW;
            return AttributedStyle.GREEN;
        }
    };

    /** 三段线性插值渐变 */
    public static class LinearGradient implements ColorGradient {
        private final int startColor, midColor, endColor;
        private final int midPoint; // 中间点百分比

        public LinearGradient(int startColor, int midColor, int endColor, int midPoint) {
            this.startColor = startColor;
            this.midColor = midColor;
            this.endColor = endColor;
            this.midPoint = Math.min(Math.max(midPoint, 1), 99);
        }

        @Override
        public int getColor(int percent) {
            if (percent <= 0) return startColor;
            if (percent >= 100) return endColor;
            if (percent < midPoint) return startColor; // 简化：前半段用起始色，后半段用结束色
            if (percent < midPoint + (100 - midPoint) / 2) return midColor;
            return endColor;
        }

        public static LinearGradient ryg() {
            return new LinearGradient(
                AttributedStyle.RED, AttributedStyle.YELLOW,
                AttributedStyle.GREEN, 50);
        }

        public static LinearGradient blueCyan() {
            return new LinearGradient(
                AttributedStyle.BLUE, AttributedStyle.CYAN,
                AttributedStyle.WHITE, 50);
        }
    }

    // ==================== 显示元素配置 ====================

    /**
     * 控制进度条右侧各显示元素的开关与格式
     */
    public static class DisplayConfig {
        boolean showPercent = true;      // 显示 " 42%"
        boolean showCount = false;       // 显示 " 42/100"
        boolean showFraction = false;    // 显示 " 0.42"
        boolean showEta = true;          // 显示 "ETA: 1m30s"
        boolean showSpeed = false;       // 显示 " 1.2 it/s"
        boolean showElapsed = false;     // 显示已用时间
        boolean colorizePercent = true;  // 百分比数字也带颜色
        String countSeparator = "/";     // 计数分隔符
        String etaPrefix = " ETA:";      // ETA 前缀
        String speedSuffix = " it/s";    // 速度后缀
        String elapsedPrefix = " [";     // 已用时间前缀
        String elapsedSuffix = "]";      // 已用时间后缀

        public DisplayConfig showPercent(boolean v) { showPercent = v; return this; }
        public DisplayConfig showCount(boolean v) { showCount = v; return this; }
        public DisplayConfig showFraction(boolean v) { showFraction = v; return this; }
        public DisplayConfig showEta(boolean v) { showEta = v; return this; }
        public DisplayConfig showSpeed(boolean v) { showSpeed = v; return this; }
        public DisplayConfig showElapsed(boolean v) { showElapsed = v; return this; }
        public DisplayConfig colorizePercent(boolean v) { colorizePercent = v; return this; }
        public DisplayConfig countSeparator(String s) { countSeparator = s; return this; }
        public DisplayConfig etaPrefix(String s) { etaPrefix = s; return this; }
        public DisplayConfig speedSuffix(String s) { speedSuffix = s; return this; }
        public DisplayConfig elapsedPrefix(String s) { elapsedPrefix = s; return this; }
        public DisplayConfig elapsedSuffix(String s) { elapsedSuffix = s; return this; }
    }

    // ==================== 字段 ====================

    private final Terminal terminal;
    private final AnsiRenderer renderer;
    private final String taskName;
    private final Style style;
    private final int barWidth;
    private final DisplayConfig display;
    private final ColorGradient colorGradient;

    private long current = 0;
    private long max = 100;
    private volatile String text = "";           // 动态文字（可通过 updateText 修改）
    private String extraMessage = "";
    private boolean started = false;
    private boolean completed = false;

    private final String widgetId;              // AnsiWidget 接口：唯一标识
    private AnsiWidgetEngine boundEngine = null; // 绑定的 Engine（null=独立模式）

    private long startTime = 0;
    private long lastUpdateTime = 0;

    // ==================== 构造器 ====================

    public AnsiProgressBar(Terminal terminal, String taskName, long max) {
        this(terminal, taskName, max, STYLE_UNICODE, new DisplayConfig(), null,
             "bar-" + System.nanoTime());
    }

    public AnsiProgressBar(Terminal terminal, String taskName, long max, Style style) {
        this(terminal, taskName, max, style, new DisplayConfig(), null,
             "bar-" + System.nanoTime());
    }

    private AnsiProgressBar(Terminal terminal, String taskName, long max,
                            Style style, DisplayConfig display, ColorGradient gradient,
                            String widgetId) {
        this.terminal = terminal;
        this.renderer = new AnsiRenderer(terminal);
        this.taskName = (taskName != null) ? taskName : "";
        this.style = (style != null) ? style : STYLE_UNICODE;
        this.display = (display != null) ? display : new DisplayConfig();
        this.colorGradient = gradient;
        this.max = max;
        this.widgetId = (widgetId != null && !widgetId.isEmpty())
            ? widgetId : "bar-" + System.nanoTime();
        // 自动计算进度条宽度
        int termWidth = Math.max(terminal.getWidth(), 40);
        int reserved = this.taskName.length() + 4 + 12 + 6; // name + brackets + percent + extras
        this.barWidth = Math.max(termWidth - reserved, 10);
    }

    /** Builder 入口 */
    public static ProgressBarBuilder builder(Terminal terminal, String taskName, long max) {
        return new ProgressBarBuilder(terminal, taskName, max);
    }

    public static class ProgressBarBuilder {
        private final Terminal terminal;
        private final String taskName;
        private final long max;
        private Style style = STYLE_UNICODE;
        private DisplayConfig display = new DisplayConfig();
        private ColorGradient gradient = null;
        private String widgetId;

        ProgressBarBuilder(Terminal terminal, String taskName, long max) {
            this.terminal = terminal;
            this.taskName = taskName;
            this.max = max;
        }

        public ProgressBarBuilder style(Style s) { style = s; return this; }
        public ProgressBarBuilder display(DisplayConfig d) { display = d; return this; }
        public ProgressBarBuilder colorGradient(ColorGradient g) { gradient = g; return this; }
        public ProgressBarBuilder widgetId(String id) { widgetId = id; return this; }

        // 快捷方法：直接操作 display
        public ProgressBarBuilder showPercent(boolean v) { display.showPercent(v); return this; }
        public ProgressBarBuilder showCount(boolean v) { display.showCount(v); return this; }
        public ProgressBarBuilder showFraction(boolean v) { display.showFraction(v); return this; }
        public ProgressBarBuilder showEta(boolean v) { display.showEta(v); return this; }
        public ProgressBarBuilder showSpeed(boolean v) { display.showSpeed(v); return this; }
        public ProgressBarBuilder showElapsed(boolean v) { display.showElapsed(v); return this; }

        public AnsiProgressBar build() {
            return new AnsiProgressBar(terminal, taskName, max, style, display, gradient,
                (widgetId != null && !widgetId.isEmpty()) ? widgetId : "bar-" + System.nanoTime());
        }
    }

    // ==================== 控制 API ====================

    /** 启动进度条 */
    public void start() {
        if (started) return;
        started = true;
        completed = false;
        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;

        if (boundEngine != null) {
            boundEngine.requestRefreshFromWidget(widgetId);
        } else {
            renderer.hideCursor();
            renderToTerminal();
        }
    }

    /** 更新当前进度值 */
    public void update(long value) {
        this.current = Math.max(0, value);
        lastUpdateTime = System.currentTimeMillis();
        if (!started) start();
        if (!completed) doRender();
    }

    /** 同时更新进度和文字 */
    public void update(long value, String newText) {
        this.text = (newText != null) ? newText : "";
        update(value);
    }

    /** 前进 +1 */
    public void step() { update(current + 1); }

    /** 前进 n */
    public void stepBy(long n) { update(current + n); }

    /** 跳到指定值 */
    public void stepTo(long value) { update(value); }

    /** 更新动态文字 */
    public void updateText(String newText) {
        this.text = (newText != null) ? newText : "";
        if (started && !completed) doRender();
    }

    /** 设置额外消息 */
    public void setExtraMessage(String msg) {
        this.extraMessage = (msg != null) ? msg : "";
        if (started && !completed) doRender();
    }

    /** 动态修改总量 */
    public void setMax(long newMax) {
        this.max = newMax;
        if (started && !completed) doRender();
    }

    /** 是否为不确定模式 */
    public boolean isIndeterminate() { return max <= 0; }

    /** 完成进度条 */
    public void complete() {
        if (completed) return;
        completed = true;
        current = isIndeterminate() ? current : max;

        if (boundEngine != null) {
            boundEngine.requestRefreshFromWidget(widgetId);
        } else {
            renderToTerminal();
            renderer.println("");
            renderer.showCursor();
        }
    }

    public void complete(String message) {
        setExtraMessage(message);
        complete();
    }

    // ==================== 渲染核心 ====================

    /**
     * 统一渲染入口：根据是否绑定 Engine 决定渲染方式
     */
    private void doRender() {
        if (boundEngine != null) {
            // Engine 模式：通知 Engine 刷新（不自己写 Terminal）
            boundEngine.requestRefreshFromWidget(widgetId);
        } else {
            // 独立模式：直接写 Terminal
            renderToTerminal();
        }
    }

    private void renderToTerminal() {
        renderer.overwriteLine(buildLine());
    }

    private AttributedString buildLine() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        int pct = getPercent();

        // [任务名]
        if (!taskName.isEmpty()) {
            sb.append(taskName, AttributedStyle.DEFAULT.bold());
            sb.append(" ");
        }

        // [左边框][进度条主体][右边框]
        sb.append(String.valueOf(style.leftBracket));
        if (isIndeterminate()) {
            renderIndeterminateBar(sb);
        } else {
            renderDeterminateBar(sb, pct);
        }
        sb.append(String.valueOf(style.rightBracket));

        // 右侧信息区
        appendRightSideInfo(sb, pct);

        return sb.toAttributedString();
    }

    private void renderDeterminateBar(AttributedStringBuilder sb, int pct) {
        int filledWidth = (int) ((long) barWidth * pct / 100);

        for (int i = 0; i < barWidth; i++) {
            int color;
            if (i < filledWidth) {
                // 已填充部分：应用颜色渐变
                if (colorGradient != null && pct > 0) {
                    int segmentPct = (int) ((long) (i + 1) * 100 / barWidth);
                    int gc = colorGradient.getColor(segmentPct);
                    color = (gc >= 0) ? gc : style.fillColor;
                } else {
                    color = style.fillColor;
                }
                sb.append(String.valueOf(style.fillChar),
                    AttributedStyle.DEFAULT.foreground(color));
            } else {
                sb.append(String.valueOf(style.emptyChar),
                    AttributedStyle.DEFAULT.foreground(style.emptyColor));
            }
        }
    }

    private void renderIndeterminateBar(AttributedStringBuilder sb) {
        long elapsed = System.currentTimeMillis() - startTime;
        int pos = (int) ((elapsed / 120) % (barWidth + 6)) - 3;

        for (int i = 0; i < barWidth; i++) {
            if (i >= pos && i < pos + 4) {
                sb.append(String.valueOf(style.fillChar),
                    AttributedStyle.DEFAULT.foreground(style.fillColor));
            } else {
                sb.append(String.valueOf(style.emptyChar),
                    AttributedStyle.DEFAULT.foreground(style.emptyColor));
            }
        }
    }

    private void appendRightSideInfo(AttributedStringBuilder sb, int pct) {
        // 百分比
        if (display.showPercent && !isIndeterminate()) {
            int clr = display.colorizePercent ? resolveColor(pct) : style.fillColor;
            sb.append(String.format(" %3d%%", pct),
                AttributedStyle.DEFAULT.foreground(clr).bold());
        }

        // 计数 (current/max)
        if (display.showCount && !isIndeterminate()) {
            int clr = resolveColor(pct);
            sb.append(String.format(" %d%s%d", current, display.countSeparator, max),
                AttributedStyle.DEFAULT.foreground(clr));
        }

        // 小数比例
        if (display.showFraction && !isIndeterminate() && max > 0) {
            double frac = (double) current / max;
            sb.append(String.format(" %.2f", frac),
                AttributedStyle.DEFAULT.foreground(resolveColor(pct)));
        }

        // 额外消息
        if (extraMessage != null && !extraMessage.isEmpty()) {
            sb.append(" ").append(extraMessage,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        }

        // 动态文字
        if (text != null && !text.isEmpty()) {
            sb.append(" ").append(text,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA));
        }

        // 速度
        if (display.showSpeed && started) {
            double speed = calcSpeed();
            if (speed >= 0) {
                sb.append(String.format(" %.1f%s", speed, display.speedSuffix),
                    AttributedStyle.DEFAULT.foreground(0));
            }
        }

        // ETA
        if (display.showEta && !isIndeterminate() && started) {
            long eta = estimateEta();
            if (eta > 0) {
                sb.append(display.etaPrefix + formatTime(eta),
                    AttributedStyle.DEFAULT.foreground(0));
            }
        }

        // 已用时间
        if (display.showElapsed && started) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            sb.append(display.elapsedPrefix + formatTime(elapsed) + display.elapsedSuffix,
                AttributedStyle.DEFAULT.foreground(0));
        }
    }

    /** 根据当前百分比解析实际颜色（考虑渐变） */
    private int resolveColor(int pct) {
        if (colorGradient != null) {
            int gc = colorGradient.getColor(pct);
            if (gc >= 0) return gc;
        }
        return style.fillColor;
    }

    // ==================== 计算辅助 ====================

    private int getPercent() {
        if (isIndeterminate()) return 0;
        if (max <= 0) return 0;
        if (current >= max) return 100;
        return (int) (current * 100 / max);
    }

    private double calcSpeed() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < 300 || current <= 0) return -1;
        return (double) current * 1000 / elapsed; // items per second
    }

    private long estimateEta() {
        if (current <= 0 || max <= 0) return -1;
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < 500) return -1;
        long remaining = max - current;
        return remaining * elapsed / current / 1000;
    }

    private static String formatTime(long seconds) {
        if (seconds < 0) return "";
        if (seconds < 60) return String.format("%ds", seconds);
        if (seconds < 3600) return String.format("%dm%02ds", seconds / 60, seconds % 60);
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return String.format("%dh%02dm", h, m);
    }

    // ==================== AnsiWidget 接口实现 ====================

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    /**
     * 渲染进度条为 AttributedString 数组（始终单行）
     * <p>
     * 此方法供 AnsiWidgetEngine 调用，返回当前状态的渲染结果。
     */
    @Override
    public AttributedString[] render() {
        return new AttributedString[]{ buildLine() };
    }

    /** 进度条始终占 1 行 */
    @Override
    public int getRenderedLineCount() {
        return 1;
    }

    /**
     * 进度条活跃条件：已启动且未完成
     * <p>
     * complete() 后 isActive() 返回 false，Engine 会自动清理。
     */
    @Override
    public boolean isActive() {
        return started && !completed;
    }

    @Override
    public void bindEngine(AnsiWidgetEngine engine) {
        this.boundEngine = engine;
    }

    @Override
    public AnsiWidgetEngine getBoundEngine() {
        return boundEngine;
    }

    // ==================== Getter ====================

    public long getCurrent() { return current; }
    public long getMax() { return max; }
    public boolean isStarted() { return started; }
    public boolean isCompleted() { return completed; }
    public DisplayConfig getDisplayConfig() { return display; }
    public AnsiRenderer getRenderer() { return renderer; }
}
