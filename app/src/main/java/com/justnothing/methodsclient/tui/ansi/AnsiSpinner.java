package com.justnothing.methodsclient.tui.ansi;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * ANSI 转圈动画控件
 * <p>
 * 单行动态渲染，通过 \r 回车 + 清行 实现原地刷新。
 * 用于表示"正在处理中"等不确定进度的场景。
 * <p>
 * 使用示例：
 * <pre>{@code
 *   AnsiSpinner spinner = new AnsiSpinner(terminal, "Loading...");
 *   spinner.start();
 *   // ... do work ...
 *   spinner.stop();
 *   spinner.succeed("Done!");  // 或 fail("Error!")
 * }</pre>
 */
public class AnsiSpinner implements AnsiWidgetEngine.AnsiWidget {

    // 预设转圈样式
    public static final String[] FRAMES_DOTS = {
        "\u28B9", "\u28BA", "\u28BB", "\u28BC", "\u28BD", "\u28BE", "\u28BF", "\u28C0",
        "\u28C1", "\u28C2", "\u28C3", "\u28C4"
    };
    public static final String[] FRAMES_LINE = {"|", "/", "-", "\\"};
    public static final String[] FRAMES_BRAILLE = {
        "\u2801", "\u2802", "\u2804", "\u2840",
        "\u2820", "\u2810", "\u2808", "\u28A0"
    };
    public static final String[] FRAMES_ARROWS = {
        "\u25B9\u25B9\u25B9", "\u25B9\u25B9\u25BF", "\u25B9\u25BF\u25BF",
        "\u25BF\u25BF\u25BF", "\u25BF\u25BF\u25B9", "\u25BF\u25B9\u25B9"
    };

    private final Terminal terminal;
    private final AnsiRenderer renderer;
    private final String[] frames;
    private final int intervalMs;
    private final String prefixText;

    private volatile boolean running = false;
    private Thread spinThread;
    private int frameIndex = 0;
    private int currentColor = AttributedStyle.CYAN;
    private volatile String dynamicText = "";  // 可动态更新的文字（线程安全）
    private final String widgetId;             // AnsiWidget 接口：唯一标识
    private volatile String endState = null;   // "succeed"/"fail"/"warn" + message（用于 Engine 渲染最终状态）

    /**
     * 创建转圈控件（默认 DOTS 样式，80ms 间隔）
     */
    public AnsiSpinner(Terminal terminal, String text) {
        this(terminal, text, FRAMES_DOTS, 80);
    }

    /**
     * 创建转圈控件
     *
     * @param terminal JLine Terminal 实例
     * @param text     转圈旁边的文字描述
     * @param frames   转圈帧序列
     * @param intervalMs 帧间隔（毫秒）
     */
    public AnsiSpinner(Terminal terminal, String text, String[] frames, int intervalMs) {
        this(terminal, text, frames, intervalMs, "spinner-" + System.nanoTime());
    }

    public AnsiSpinner(Terminal terminal, String text, String[] frames,
                       int intervalMs, String widgetId) {
        this.terminal = terminal;
        this.renderer = new AnsiRenderer(terminal);
        this.frames = frames;
        this.intervalMs = intervalMs;
        this.prefixText = (text != null && !text.isEmpty()) ? text + " " : "";
        this.widgetId = (widgetId != null && !widgetId.isEmpty())
            ? widgetId : "spinner-" + System.nanoTime();
    }

    /** 开始转圈动画 */
    public void start() {
        if (running) return;
        running = true;
        frameIndex = 0;
        renderer.hideCursor();

        spinThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                String frame = frames[frameIndex % frames.length];
                // 组合文字：初始前缀 + 动态更新文字
                String text = prefixText;
                if (!dynamicText.isEmpty()) {
                    text = dynamicText;
                }
                AttributedString line = new AttributedString(
                    frame + " " + text,
                    AttributedStyle.DEFAULT.foreground(currentColor)
                );
                renderer.overwriteLine(line);
                frameIndex++;
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "AnsiSpinner");
        spinThread.setDaemon(true);
        spinThread.start();
    }

    /** 停止转圈动画 */
    public void stop() {
        running = false;
        if (spinThread != null) {
            spinThread.interrupt();
            try {
                spinThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            spinThread = null;
        }
    }

    /**
     * 停止并显示成功标记
     * 输出类似：✔ Done!
     */
    public void succeed(String message) {
        stop();
        this.endState = "succeed:" + (message != null ? message : "");
        String text = message != null ? message : "";
        AttributedString line = new AttributedString(
            "\u2714 " + text,
            AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold()
        );
        renderer.overwriteLine(line);
        renderer.println(""); // 换行释放该行
        renderer.showCursor();
    }

    /**
     * 停止并显示失败标记
     * 输出类似：✖ Error!
     */
    public void fail(String message) {
        stop();
        this.endState = "fail:" + (message != null ? message : "");
        String text = message != null ? message : "";
        AttributedString line = new AttributedString(
            "\u2716 " + text,
            AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold()
        );
        renderer.overwriteLine(line);
        renderer.println("");
        renderer.showCursor();
    }

    /**
     * 停止并显示警告标记
     */
    public void warn(String message) {
        stop();
        this.endState = "warn:" + (message != null ? message : "");
        String text = message != null ? message : "";
        AttributedString line = new AttributedString(
            "\u26A0 " + text,
            AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold()
        );
        renderer.overwriteLine(line);
        renderer.println("");
        renderer.showCursor();
    }

    /** 更新文字内容（线程安全，下次渲染帧生效） */
    public void updateText(String newText) {
        this.dynamicText = (newText != null) ? newText : "";
    }

    /** 设置转圈颜色 */
    public void setColor(int ansiColor) {
        this.currentColor = ansiColor;
    }

    public boolean isRunning() {
        return running;
    }

    // ==================== AnsiWidget 接口实现 ====================

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    /**
     * 渲染转圈当前状态
     * <p>
     * 如果有 endState（succeed/fail/warn），返回最终状态行。
     * 否则返回当前帧 + 文字。
     */
    @Override
    public AttributedString[] render() {
        if (endState != null) {
            return new AttributedString[]{ renderEndState() };
        }
        String frame = frames[frameIndex % frames.length];
        String text = dynamicText.isEmpty() ? prefixText : dynamicText;
        AttributedString line = new AttributedString(
            frame + " " + text,
            AttributedStyle.DEFAULT.foreground(currentColor)
        );
        return new AttributedString[]{ line };
    }

    /** 转圈始终占 1 行 */
    @Override
    public int getRenderedLineCount() {
        return 1;
    }

    /**
     * 转圈活跃条件：正在运行（未 stop/succeed/fail）
     */
    @Override
    public boolean isActive() {
        return running || endState != null;  // endState 非空表示刚结束但还未被清理
    }

    private AttributedString renderEndState() {
        if (endState == null) return new AttributedString("");
        if (endState.startsWith("succeed:")) {
            String msg = endState.substring("succeed:".length());
            return new AttributedString(
                "\u2714 " + msg,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold()
            );
        } else if (endState.startsWith("fail:")) {
            String msg = endState.substring("fail:".length());
            return new AttributedString(
                "\u2716 " + msg,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold()
            );
        } else if (endState.startsWith("warn:")) {
            String msg = endState.substring("warn:".length());
            return new AttributedString(
                "\u26A0 " + msg,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold()
            );
        }
        return new AttributedString(endState);
    }
}
