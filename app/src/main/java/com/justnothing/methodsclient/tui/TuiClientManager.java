package com.justnothing.methodsclient.tui;

import com.justnothing.methodsclient.utils.TerminalManager;
import com.justnothing.methodsclient.tui.ansi.AnsiProgressBar;
import com.justnothing.methodsclient.tui.ansi.AnsiRenderer;
import com.justnothing.methodsclient.tui.ansi.AnsiSpinner;
import com.justnothing.methodsclient.tui.ansi.AnsiWidgetEngine;
import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.tui.TuiWidgetData;
import com.justnothing.testmodule.command.tui.TuiWidgetType;
import com.justnothing.testmodule.command.tui.config.ProgressBarConfig;
import com.justnothing.testmodule.command.tui.config.SpinnerConfig;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 客户端 TUI Widget 管理器（单例）
 * <p>
 * 接收服务端通过 InteractiveProtocol 发来的 Widget 命令（0x18-0x1B），
 * 在本地 Android Terminal 上创建/更新/销毁实际的 ANSI Widget。
 */
public class TuiClientManager {

    private static final Logger LOG = Logger.getLogger("TuiClientManager");
    private static volatile TuiClientManager instance;

    private AnsiWidgetEngine engine;
    private final Map<String, Object> liveWidgets = new ConcurrentHashMap<>();
    private boolean active = false;

    // 私有构造
    private TuiClientManager() {}

    public static synchronized TuiClientManager getInstance() {
        if (instance == null) {
            instance = new TuiClientManager();
        }
        return instance;
    }

    /** 获取当前是否活跃（有 Widget 正在显示） */
    public boolean isActive() { return active; }

    /**
     * 处理来自服务端的 Widget 创建命令
     * @param data JSON 序列化的 TuiWidgetData 字节数组
     */
    public void handleWidgetCreate(byte[] data) {
        log("handleWidgetCreate: received %d bytes", data != null ? data.length : 0);
        if (data == null || data.length == 0) {
            log("  data is empty, ignoring");
            return;
        }
        try {
            TuiWidgetData widgetData = TuiWidgetData.fromJsonBytes(data);
            log("  widgetId=%s, type=%s, title=%s",
                widgetData.getWidgetId(), widgetData.getType(), widgetData.getTitle());

            ensureEngineStarted();

            switch (widgetData.getType()) {
                case PROGRESS_BAR:
                    createProgressBar(widgetData);
                    break;
                case SPINNER:
                    createSpinner(widgetData);
                    break;
                default:
                    log("  unsupported widget type: %s", widgetData.getType());
                    break;
            }
        } catch (Exception e) {
            log("ERROR handleWidgetCreate: %s", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * 处理来自服务端的 Widget 更新命令
     */
    public void handleWidgetUpdate(byte[] data) {
        log("handleWidgetUpdate: received %d bytes", data != null ? data.length : 0);
        if (data == null || data.length == 0) return;
        try {
            TuiWidgetData widgetData = TuiWidgetData.fromJsonBytes(data);
            String id = widgetData.getWidgetId();
            log("  updating widget: %s", id);

            Object widget = liveWidgets.get(id);
            if (widget instanceof AnsiProgressBar) {
                updateProgressBar((AnsiProgressBar) widget, widgetData);
            } else if (widget instanceof AnsiSpinner) {
                updateSpinner((AnsiSpinner) widget, widgetData);
            } else {
                log("  widget not found or wrong type: %s", id);
            }
        } catch (Exception e) {
            log("ERROR handleWidgetUpdate: %s", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /** 处理销毁单个 Widget */
    public void handleWidgetDestroy(String widgetId) {
        log("handleWidgetDestroy: %s", widgetId);
        if (widgetId == null) return;
        Object removed = liveWidgets.remove(widgetId);
        if (removed != null && engine != null) {
            engine.removeWidget(widgetId);
        }
        checkStopEngine();
    }

    /** 清除所有 Widget */
    public void handleClearAll() {
        log("handleClearAll");
        if (engine != null) {
            engine.stop();
            engine = null;
        }
        liveWidgets.clear();
        active = false;
    }

    // ==================== 内部实现 ====================

    private void ensureEngineStarted() {
        if (engine != null && engine.isStarted()) return;
        try {
            Terminal terminal = TerminalManager.getTerminal();
            log("  creating AnsiWidgetEngine with terminal type=%s", terminal.getClass().getSimpleName());
            engine = new AnsiWidgetEngine(terminal);
            engine.setDebugLogging(true);
            engine.start();
            active = true;
            log("  AnsiWidgetEngine started successfully");
        } catch (Exception e) {
            log("ERROR failed to start AnsiWidgetEngine: %s", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void createProgressBar(TuiWidgetData widgetData) {
        String id = widgetData.getWidgetId();
        if (liveWidgets.containsKey(id)) {
            log("  progressBar %s already exists, skipping create", id);
            return;  // 已存在则跳过，后续用 update
        }

        ProgressBarConfig config = null;
        if (widgetData.getPayload() instanceof ProgressBarConfig) {
            config = (ProgressBarConfig) widgetData.getPayload();
        } else if (widgetData.getPayload() instanceof Map) {
            // Gson 反序列化可能返回 Map，手动转换
            config = mapToProgressBarConfig((Map<?, ?>) widgetData.getPayload());
        }

        if (config == null) {
            config = new ProgressBarConfig();  // 使用默认值
        }

        try {
            Terminal terminal = engine.getTerminal();
            AnsiProgressBar.Style style = resolveStyle(config.getStyle());
            
            AnsiProgressBar bar = AnsiProgressBar.builder(terminal,
                    config.getTaskName() != null ? config.getTaskName() : widgetData.getTitle(),
                    config.getMax())
                .style(style)
                .widgetId(id)
                .showPercent(config.isShowPercent())
                .showCount(config.isShowCount())
                .showEta(config.isShowEta())
                .showSpeed(config.isShowSpeed())
                .showElapsed(config.isShowElapsed())
                .build();

            // 设置初始值
            if (config.getCurrent() > 0) {
                // 通过反射或直接设置内部状态... 
                // 更简单的方式：直接调用 update
            }
            if (config.getText() != null && !config.getText().isEmpty()) {
                bar.updateText(config.getText());
            }
            if (config.getExtraMessage() != null && !config.getExtraMessage().isEmpty()) {
                bar.setExtraMessage(config.getExtraMessage());
            }

            engine.addWidget(bar);
            liveWidgets.put(id, bar);
            bar.start();
            log("  created progressBar: %s (max=%d)", id, config.getMax());
        } catch (Exception e) {
            log("ERROR createProgressBar: %s", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void createSpinner(TuiWidgetData widgetData) {
        String id = widgetData.getWidgetId();
        if (liveWidgets.containsKey(id)) {
            log("  spinner %s already exists, skipping create", id);
            return;
        }

        SpinnerConfig config = null;
        if (widgetData.getPayload() instanceof SpinnerConfig) {
            config = (SpinnerConfig) widgetData.getPayload();
        } else if (widgetData.getPayload() instanceof Map) {
            config = mapToSpinnerConfig((Map<?, ?>) widgetData.getPayload());
        }
        if (config == null) config = new SpinnerConfig();

        try {
            Terminal terminal = engine.getTerminal();
            String[] frames = resolveFrames(config.getFrameStyle());
            int color = config.getColor() > 0 ? config.getColor() : AttributedStyle.CYAN;

            AnsiSpinner spinner = new AnsiSpinner(terminal,
                    config.getText() != null ? config.getText() : widgetData.getTitle(),
                    frames, config.getIntervalMs(), id);

            spinner.setColor(color);
            if (config.getDynamicText() != null && !config.getDynamicText().isEmpty()) {
                spinner.updateText(config.getDynamicText());
            }

            engine.addWidget(spinner);
            liveWidgets.put(id, spinner);
            spinner.start();
            log("  created spinner: %s", id);
        } catch (Exception e) {
            log("ERROR createSpinner: %s", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void updateProgressBar(AnsiProgressBar bar, TuiWidgetData widgetData) {
        ProgressBarConfig config = null;
        if (widgetData.getPayload() instanceof ProgressBarConfig) {
            config = (ProgressBarConfig) widgetData.getPayload();
        } else if (widgetData.getPayload() instanceof Map) {
            config = mapToProgressBarConfig((Map<?, ?>) widgetData.getPayload());
        }
        if (config == null) return;

        bar.update(config.getCurrent());
        if (config.getText() != null) bar.updateText(config.getText());
        if (config.getExtraMessage() != null) bar.setExtraMessage(config.getExtraMessage());
        if (config.getMax() > 0 && config.getMax() != bar.getMax()) bar.setMax(config.getMax());
        
        // 检查是否完成（current >= max 且有结束标记意图）
        if (config.getCurrent() >= config.getMax()) {
            bar.complete();
        }
    }

    private void updateSpinner(AnsiSpinner spinner, TuiWidgetData widgetData) {
        SpinnerConfig config = null;
        if (widgetData.getPayload() instanceof SpinnerConfig) {
            config = (SpinnerConfig) widgetData.getPayload();
        } else if (widgetData.getPayload() instanceof Map) {
            config = mapToSpinnerConfig((Map<?, ?>) widgetData.getPayload());
        }
        if (config == null) return;

        if (config.getDynamicText() != null) spinner.updateText(config.getDynamicText());
        if (config.getColor() > 0) spinner.setColor(config.getColor());
    }

    private void checkStopEngine() {
        if (!liveWidgets.isEmpty()) return;
        // 所有 widget 都清理完了，停止 engine
        if (engine != null) {
            engine.stop();
            engine = null;
        }
        active = false;
    }

    // ==================== 辅助方法 ====================

    private AnsiProgressBar.Style resolveStyle(String styleName) {
        if (styleName == null || styleName.isEmpty()) return AnsiProgressBar.STYLE_UNICODE;
        return switch (styleName.toLowerCase()) {
            case "ascii" -> AnsiProgressBar.STYLE_ASCII;
            case "block" -> AnsiProgressBar.STYLE_BLOCK;
            case "colorful" -> AnsiProgressBar.STYLE_COLORFUL;
            default -> AnsiProgressBar.STYLE_UNICODE;
        };
    }

    private String[] resolveFrames(String frameStyle) {
        if (frameStyle == null || frameStyle.isEmpty()) return AnsiSpinner.FRAMES_DOTS;
        return switch (frameStyle.toLowerCase()) {
            case "line" -> AnsiSpinner.FRAMES_LINE;
            case "braille" -> AnsiSpinner.FRAMES_BRAILLE;
            case "arrows" -> AnsiSpinner.FRAMES_ARROWS;
            default -> AnsiSpinner.FRAMES_DOTS;
        };
    }

    @SuppressWarnings("unchecked")
    private ProgressBarConfig mapToProgressBarConfig(Map<?, ?> map) {
        ProgressBarConfig c = new ProgressBarConfig();
        if (map.containsKey("taskName")) c.setTaskName(String.valueOf(map.get("taskName")));
        if (map.containsKey("max")) c.setMax(((Number) map.get("max")).longValue());
        if (map.containsKey("current")) c.setCurrent(((Number) map.get("current")).longValue());
        if (map.containsKey("style")) c.setStyle(String.valueOf(map.get("style")));
        if (map.containsKey("showPercent")) c.setShowPercent(Boolean.TRUE.equals(map.get("showPercent")));
        if (map.containsKey("showCount")) c.setShowCount(Boolean.TRUE.equals(map.get("showCount")));
        if (map.containsKey("showEta")) c.setShowEta(Boolean.TRUE.equals(map.get("showEta")));
        if (map.containsKey("showSpeed")) c.setShowSpeed(Boolean.TRUE.equals(map.get("showSpeed")));
        if (map.containsKey("showElapsed")) c.setShowElapsed(Boolean.TRUE.equals(map.get("showElapsed")));
        if (map.containsKey("text")) c.setText(String.valueOf(map.get("text")));
        if (map.containsKey("extraMessage")) c.setExtraMessage(String.valueOf(map.get("extraMessage")));
        return c;
    }

    @SuppressWarnings("unchecked")
    private SpinnerConfig mapToSpinnerConfig(Map<?, ?> map) {
        SpinnerConfig c = new SpinnerConfig();
        if (map.containsKey("text")) c.setText(String.valueOf(map.get("text")));
        if (map.containsKey("frameStyle")) c.setFrameStyle(String.valueOf(map.get("frameStyle")));
        if (map.containsKey("intervalMs")) c.setIntervalMs(((Number) map.get("intervalMs")).intValue());
        if (map.containsKey("color")) c.setColor(((Number) map.get("color")).intValue());
        if (map.containsKey("dynamicText")) c.setDynamicText(String.valueOf(map.get("dynamicText")));
        return c;
    }

    private void log(String fmt, Object... args) {
        String msg = String.format(fmt, args);
        LOG.info("[TUI-Client] " + msg);
    }
}
