package com.justnothing.methodsclient.tui.ansi;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ANSI Widget 引擎（Rich 风格：Widget 固定在底部）
 * <p>
 * 核心布局：
 * <pre>
 *   [Log Line 1]          ← 正常输出（追加）
 *   [Log Line 2]              在 Widget 区域上方
 *   ─────────────           ← 分隔线（可选）
 *   [Progress Bar 50%]     ← Widget 区域（固定在底部）
 *   [Spinner ◷ loading]
 * </pre>
 * <p>
 * 工作原理：
 * <ol>
 *   <li>所有 Widget 注册到 Engine，Engine 计算总行数</li>
 *   <li>所有 println/log 调用走 Engine 的 {@link #printLine(String)} 方法</li>
 *   <li>打印时：上移光标到 Widget 区域上方 → 输出新行 → 重绘所有 Widget</li>
 *   <li>Widget 的 start()/update() 只更新内部状态，由 Engine 统一渲染</li>
 * </ol>
 */
public class AnsiWidgetEngine {

    private static final Logger LOG = Logger.getLogger("AnsiWidgetEngine");

    private final Terminal terminal;
    private final AnsiRenderer renderer;
    private final List<AnsiWidget> widgets = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private boolean started = false;
    private int totalWidgetLines = 0;
    private boolean showSeparator = true;
    private boolean debugLogging = true;       // 诊断日志开关

    /**
     * Widget 抽象接口：所有动态控件必须实现此接口才能被 Engine 管理
     */
    public interface AnsiWidget {
        /** 获取控件唯一 ID */
        String getWidgetId();

        /** 渲染当前状态，返回每行的 AttributedString 数组 */
        AttributedString[] render();

        /** 控件当前占用多少显示行 */
        int getRenderedLineCount();

        /** 控件是否还在活跃（false 时 Engine 可移除） */
        boolean isActive();

        /**
         * 将此 Widget 绑定到 Engine。
         * <p>
         * 绑定后，Widget 的 start()/update()/complete() 等方法应只更新内部状态，
         * 不再直接写 Terminal，而是通过 Engine 统一调度渲染。
         * 如果未绑定（null），Widget 处于独立模式，自行管理 Terminal 输出。
         */
        default void bindEngine(AnsiWidgetEngine engine) {}

        /** 获取绑定的 Engine（可能为 null） */
        default AnsiWidgetEngine getBoundEngine() { return null; }
    }

    // ==================== 构造与生命周期 ====================

    public AnsiWidgetEngine(Terminal terminal) {
        this.terminal = terminal;
        this.renderer = new AnsiRenderer(terminal);
    }

    public Terminal getTerminal() { return terminal; }
    public AnsiRenderer getRenderer() { return renderer; }
    public boolean isStarted() { return started; }

    /** 开启/关闭诊断日志 */
    public void setDebugLogging(boolean enabled) { this.debugLogging = enabled; }

    /**
     * 启动引擎（隐藏光标，准备接收 Widget 和输出）
     */
    public void start() {
        if (started) return;
        started = true;
        log("Engine.start() — hiding cursor");
        renderer.hideCursor();
        recalculateLayout();
    }

    /**
     * 停止引擎（显示光标，清理 Widget 区域）
     */
    public void stop() {
        if (!started) return;
        log("Engine.stop() — cleaning up");
        started = false;

        lock.lock();
        try {
            if (totalWidgetLines > 0) {
                log("  clearing %d widget lines", totalWidgetLines);
                renderer.getWriter().print(AnsiRenderer.cursorUp(totalWidgetLines));
                for (int i = 0; i < totalWidgetLines + (showSeparator ? 1 : 0); i++) {
                    renderer.getWriter().print(AnsiRenderer.CLEAR_LINE + "\n");
                }
                renderer.getWriter().flush();
            }
            totalWidgetLines = 0;
            // 解绑所有 Widget
            for (AnsiWidget w : widgets) {
                w.bindEngine(null);
            }
            widgets.clear();
        } finally {
            lock.unlock();
        }
        renderer.showCursor();
    }

    // ==================== Widget 管理 ====================

    /**
     * 注册一个 Widget 到 Engine
     * <p>
     * 注册后自动绑定：Widget 的渲染完全由 Engine 接管，
     * Widget 自身不应再直接调用 Terminal.writer() 输出。
     */
    public void addWidget(AnsiWidget widget) {
        lock.lock();
        try {
            for (AnsiWidget w : widgets) {
                if (w.getWidgetId().equals(widget.getWidgetId())) {
                    log("addWidget: %s already registered, skipping", widget.getWidgetId());
                    return;
                }
            }
            widgets.add(widget);
            // ★ 绑定：告诉 Widget "你归我管了，别自己写 Terminal"
            widget.bindEngine(this);
            recalculateLayoutInternal();
            log("addWidget: %s bound, totalWidgets=%d, totalLines=%d",
                widget.getWidgetId(), widgets.size(), totalWidgetLines);
            refreshWidgets();  // 立即刷新显示新 Widget
        } finally {
            lock.unlock();
        }
    }

    /** 按 ID 移除一个 Widget */
    public void removeWidget(String widgetId) {
        lock.lock();
        try {
            widgets.removeIf(w -> {
                if (w.getWidgetId().equals(widgetId)) {
                    w.bindEngine(null);  // 解绑
                    return true;
                }
                return false;
            });
            recalculateLayoutInternal();
            refreshWidgets();
        } finally {
            lock.unlock();
        }
    }

    /** 移除所有已不活跃的 Widget */
    public void cleanupInactiveWidgets() {
        lock.lock();
        try {
            widgets.removeIf(w -> {
                if (!w.isActive()) {
                    w.bindEngine(null);
                    return true;
                }
                return false;
            });
            recalculateLayoutInternal();
            refreshWidgets();
        } finally {
            lock.unlock();
        }
    }

    public int getWidgetCount() { return widgets.size(); }

    public void setShowSeparator(boolean show) { this.showSeparator = show; }

    // ==================== 安全输出 API ====================

    /**
     * 打印一行文字，自动在 Widget 区域上方输出
     */
    public void printLine(String text) {
        if (!started) {
            renderer.println(text);
            return;
        }
        lock.lock();
        try {
            safePrintLine(text);
        } finally {
            lock.unlock();
        }
    }

    public void printLine(String text, byte colorByte) {
        printLine(AnsiRenderer.colored(text, colorByte));
    }

    public void printLine(AttributedString text) {
        if (!started) {
            renderer.println(text);
            return;
        }
        lock.lock();
        try {
            safePrintAttributedLine(text);
        } finally {
            lock.unlock();
        }
    }

    public void printRaw(String text) {
        renderer.print(text);
    }

    // ==================== Widget 刷新（供绑定后的 Widget 调用）====================

    /**
     * Widget 状态变化后请求刷新
     * <p>
     * 这是给已绑定 Widget 调用的唯一入口。
     * 未绑定时调用无效（避免独立模式下误触发）。
     */
    public void requestRefreshFromWidget(String widgetId) {
        if (!started) return;
        log("requestRefreshFromWidget: %s", widgetId);
        lock.lock();
        try {
            doRefreshWidgets();
        } finally {
            lock.unlock();
        }
    }

    /** 外部手动触发全量刷新 */
    public void refreshWidgets() {
        if (!started) return;
        log("refreshWidgets() called");
        lock.lock();
        try {
            doRefreshWidgets();
        } finally {
            lock.unlock();
        }
    }

    // ==================== 内部实现 ====================

    private void safePrintLine(String text) {
        int areaLines = getTotalAreaLines();
        log("safePrintLine: areaLines=%d, text='%s'", areaLines,
            text.length() > 40 ? text.substring(0, 40) + "..." : text);

        if (areaLines > 0) {
            renderer.getWriter().print(AnsiRenderer.cursorUp(areaLines));
        }
        renderer.getWriter().print(AnsiRenderer.CLEAR_LINE + text + "\n");
        doRefreshWidgets();
    }

    private void safePrintAttributedLine(AttributedString text) {
        int areaLines = getTotalAreaLines();
        if (areaLines > 0) {
            renderer.getWriter().print(AnsiRenderer.cursorUp(areaLines));
        }
        renderer.getWriter().print(AnsiRenderer.CLEAR_LINE);
        text.println(terminal);
        doRefreshWidgets();
    }

    /**
     * 执行 Widget 区域的完整重绘（核心方法）
     */
    private void doRefreshWidgets() {
        int oldLines = totalWidgetLines;
        int newLines = calculateTotalWidgetLines();

        log("doRefreshWidgets: oldLines=%d, newLines=%d, widgets=%d",
            oldLines, newLines, widgets.size());

        // 上移并清除旧内容
        if (oldLines > 0) {
            int upCount = oldLines + (showSeparator && !widgets.isEmpty() ? 1 : 0);
            log("  moving cursor up %d lines", upCount);
            renderer.getWriter().print(AnsiRenderer.cursorUp(upCount));
        }

        // 绘制分隔线
        if (showSeparator && !widgets.isEmpty()) {
            String sep = "\u2500".repeat(Math.min(Math.max(terminal.getWidth(), 20), 40));
            log("  drawing separator: '%s'", sep);
            renderer.getWriter().print(AnsiRenderer.CLEAR_LINE + sep + "\n");
        }

        // 绘制每个 Widget
        for (int wi = 0; wi < widgets.size(); wi++) {
            AnsiWidget widget = widgets.get(wi);
            AttributedString[] lines = widget.render();
            log("  rendering widget[%d]: id=%s, lines=%d",
                wi, widget.getWidgetId(), lines != null ? lines.length : 0);
            if (lines != null) {
                for (int li = 0; li < lines.length; li++) {
                    AttributedString line = lines[li];
                    String preview = line != null ? line.toString() : "(null)";
                    if (preview.length() > 60) preview = preview.substring(0, 60) + "...";
                    log("    line[%d]: '%s'", li, preview);
                    renderer.getWriter().print(AnsiRenderer.CLEAR_LINE);
                    if (line != null) {
                        line.println(terminal);
                    } else {
                        renderer.getWriter().println();
                    }
                }
            }
        }
        renderer.getWriter().flush();

        totalWidgetLines = newLines;
        log("doRefreshWidgets DONE: totalWidgetLines=%d", totalWidgetLines);
    }

    private int getTotalAreaLines() {
        int lines = totalWidgetLines;
        if (showSeparator && !widgets.isEmpty()) lines++;
        return lines;
    }

    private int calculateTotalWidgetLines() {
        int total = 0;
        for (AnsiWidget w : widgets) {
            total += w.getRenderedLineCount();
        }
        return total;
    }

    private void recalculateLayout() {
        lock.lock();
        try { recalculateLayoutInternal(); } finally { lock.unlock(); }
    }

    private void recalculateLayoutInternal() {
        totalWidgetLines = calculateTotalWidgetLines();
    }

    // ==================== 日志 ====================

    private void log(String fmt, Object... args) {
        if (!debugLogging) return;
        String msg = String.format(fmt, args);
        LOG.info("[TUI-Engine] " + msg);
    }
}
