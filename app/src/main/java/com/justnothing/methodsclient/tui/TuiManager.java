package com.justnothing.methodsclient.tui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.justnothing.methodsclient.tui.widget.TuiWidget;
import com.justnothing.methodsclient.tui.widget.TuiWidgetData;
import com.justnothing.methodsclient.tui.widget.TuiWidgetType;
import com.justnothing.methodsclient.tui.widget.TuiProgressBar;
import com.justnothing.methodsclient.tui.widget.TuiLogPanel;
import com.justnothing.methodsclient.tui.widget.TuiStatusBar;
import com.justnothing.methodsclient.tui.widget.TuiTable;
import com.justnothing.methodsclient.tui.widget.TuiSpinner;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TUI 管理器 — 基于 Lanterna 的终端 UI 引擎核心。
 * <p>
 * 职责：
 * <ul>
 *     <li>管理 Lanterna {@link Screen} 生命周期（启动/暂停/停止）</li>
 *     <li>维护组件注册表（widgetId → TuiWidget 实例）</li>
 *     <li>处理服务端通过 InteractiveProtocol 发送的 CREATE/UPDATE/DESTROY/CLEAR_ALL 消息</li>
 *     <li>提供线程安全的组件操作接口（协议消息来自 Socket 线程，Screen 操作在主线程）</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *   // 启动 TUI（接管终端）
 *   TuiManager tui = new TuiManager();
 *   tui.start();
 *
 *   // 服务端发送消息时调用
 *   tui.handleWidgetCreate(data);
 *   tui.handleWidgetUpdate(data);
 *
 *   // 停止 TUI（释放终端）
 *   tui.stop();
 * </pre>
 */
public class TuiManager {

    private static final Logger logger = Logger.getLoggerForName("TuiManager");

    /** 组件注册表：widgetId → 组件实例 */
    private final Map<String, TuiWidget> widgets = new ConcurrentHashMap<>();

    private Screen screen;
    private MultiWindowTextGUI gui;
    private Panel rootPanel;
    private Panel widgetPanel;      // 上方动态区域
    private Panel inputPanel;       // 下方输入区域

    private TextBox inputTextBox;
    private Label statusLabel;
    private boolean running = false;

    // ==================== 生命周期 ====================

    /**
     * 启动 TUI 引擎，初始化 Lanterna Screen 和 GUI。
     * <p>
     * 此方法会接管终端控制权，所有后续输出都通过 Screen 进行。
     */
    public synchronized void start() {
        if (running) return;

        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            factory.setForceTextTerminal(true);  // 不使用 Swing/AWT
            screen = factory.createScreen();
            screen.startScreen();

            // 清屏
            screen.clear();

            // 创建 GUI 层
            gui = new MultiWindowTextGUI(screen);

            // 构建布局：上方组件区 + 下方输入行
            buildLayout();

            running = true;
            logger.info("TUI 引擎启动成功, 终端尺寸: %s", screen.getTerminalSize());

        } catch (Exception e) {
            logger.error("TUI 启动失败", e);
            throw new RuntimeException("Failed to start TUI engine", e);
        }
    }

    /**
     * 停止 TUI 引擎，释放终端控制权。
     */
    public synchronized void stop() {
        if (!running) return;

        try {
            // 销毁所有组件
            clearAllWidgets();

            if (gui != null) {
                gui.removeWindow(gui.getActiveWindow());
            }
            if (screen != null) {
                screen.stopScreen();
            }
            running = false;
            logger.info("TUI 引擎已停止");

        } catch (Exception e) {
            logger.error("TUI 停止时出错", e);
        }
    }

    /**
     * 暂停 TUI 渲染（临时交出终端控制权，如执行外部命令时）
     */
    public synchronized void pause() {
        if (!running || screen == null) return;
        try {
            screen.stopScreen();  // 注意：stopScreen 会恢复终端原始状态
            logger.debug("TUI 已暂停");
        } catch (Exception e) {
            logger.error("TUI 暂停失败", e);
        }
    }

    /**
     * 恢复 TUI 渲染（重新获取终端控制权）
     */
    public synchronized void resume() {
        if (!running || screen == null) return;
        try {
            screen.startScreen();
            refresh();
            logger.debug("TUI 已恢复");
        } catch (Exception e) {
            logger.error("TUI 恢复失败", e);
        }
    }

    // ==================== 布局构建 ====================

    private void buildLayout() {
        // 根面板：垂直分割
        rootPanel = new Panel(new LinearLayout(Direction.VERTICAL));

        // 上方：动态组件区域（占据剩余空间）
        widgetPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        widgetPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        rootPanel.addComponent(widgetPanel.withBorder(Borders.singleLine(" Output ")));

        // 分隔线
        rootPanel.addComponent(new Separator(Direction.HORIZONTAL));

        // 下方：输入区域（固定高度）
        inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        inputTextBox = new TextBox().setHorizontalFocusSwitching(true);
        inputTextBox.addTo(inputPanel);

        // 状态标签（右侧显示当前模式等）
        statusLabel = new Label("");
        statusLabel.addTo(inputPanel);

        rootPanel.addComponent(inputPanel);

        // 创建主窗口
        BasicWindow mainWindow = new BasicWindow("TestModule REPL");
        mainWindow.setComponent(rootPanel);
        mainWindow.setHints(java.util.Arrays.asList(Window.Hint.FULL_SCREEN));

        gui.addWindow(mainWindow);
    }

    // ==================== 协议消息处理 ====================

    /**
     * 处理 TYPE_TUI_WIDGET_CREATE 消息。
     * 根据数据中的 type 字段创建对应类型的组件实例并注册到面板中。
     */
    public void handleWidgetCreate(TuiWidgetData data) {
        if (data == null) return;

        String widgetId = data.getId();
        TuiWidgetType type = data.getWidgetType();

        if (type == null) {
            logger.warn("创建组件失败: 未知类型 '%s'", data.getType());
            return;
        }

        // 如果已存在同 ID 组件，先销毁
        if (widgets.containsKey(widgetId)) {
            handleWidgetDestroy(widgetId);
        }

        try {
            TuiWidget widget = createWidget(type, data);
            if (widget != null) {
                widgets.put(widgetId, widget);

                // 将组件添加到面板
                Component component = widget.getComponent();
                if (component != null) {
                    widgetPanel.addComponent(component);
                    refresh();
                }

                logger.info("创建 TUI 组件: id=%s, type=%s, title=%s",
                        widgetId, type.getName(), data.getTitle());
            }
        } catch (Exception e) {
            logger.error("创建 TUI 组件异常: id=" + widgetId + ", type=" + type, e);
        }
    }

    /**
     * 处理 TYPE_TUI_WIDGET_UPDATE 消息。
     * 找到对应组件并更新其状态。
     */
    public void handleWidgetUpdate(TuiWidgetData data) {
        if (data == null) return;

        TuiWidget widget = widgets.get(data.getId());
        if (widget == null) {
            logger.warn("更新组件失败: 未找到 id='%s'", data.getId());
            return;
        }

        try {
            widget.update(data);
            refresh();
        } catch (Exception e) {
            logger.error("更新 TUI 组件异常: id=" + data.getId(), e);
        }
    }

    /**
     * 处理 TYPE_TUI_WIDGET_DESTROY 消息。
     * 销毁指定组件并从面板移除。
     */
    public void handleWidgetDestroy(String widgetId) {
        TuiWidget widget = widgets.remove(widgetId);
        if (widget == null) return;

        try {
            widget.dispose();

            // 从面板中移除组件
            Component component = widget.getComponent();
            if (component != null) {
                widgetPanel.removeComponent(component);
                refresh();
            }

            logger.info("销毁 TUI 组件: id=%s", widgetId);
        } catch (Exception e) {
            logger.error("销毁 TUI 组件异常: id=" + widgetId, e);
        }
    }

    /**
     * 处理 TYPE_TUI_CLEAR_ALL 消息。
     * 销毁所有组件（通常在命令结束时调用）。
     */
    public void clearAllWidgets() {
        for (String widgetId : new LinkedHashMap<>(widgets).keySet()) {
            handleWidgetDestroy(widgetId);
        }
        widgets.clear();
        refresh();
    }

    // ==================== 输入处理 ====================

    /**
     * 获取用户当前输入的文本内容
     */
    public String getInputText() {
        if (inputTextBox != null) {
            return inputTextBox.getText();
        }
        return "";
    }

    /**
     * 设置输入框的提示文本
     */
    public void setPrompt(String prompt) {
        // Lanterna 3.1.1 TextBox 不支持 placeholder，暂不处理
    }

    /**
     * 清空输入框内容
     */
    public void clearInput() {
        if (inputTextBox != null) {
            inputTextBox.setText("");
        }
    }

    /**
     * 更新状态栏文本
     */
    public void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 工厂方法：根据类型创建对应的 TuiWidget 实例
     */
    private TuiWidget createWidget(TuiWidgetType type, TuiWidgetData data) {
        switch (type.getCode()) {
            case 1: return new TuiProgressBar(data);   // PROGRESS_BAR
            case 2: return new TuiLogPanel(data);       // LOG_PANEL
            case 3: return new TuiStatusBar(data);      // STATUS_BAR
            case 4: return new TuiTable(data);          // TABLE
            case 5: return new TuiSpinner(data);        // SPINNER
            default:
                logger.warn("不支持的组件类型: %s (%d)", type.getName(), type.getCode());
                return null;
        }
    }

    /**
     * 刷新屏幕显示
     */
    void refresh() {
        if (screen != null && running) {
            try {
                screen.refresh();
            } catch (Exception e) {
                // 忽略刷新异常（可能在非活跃状态下调用）
            }
        }
    }

    // ==================== 查询接口 ====================

    /** 是否正在运行 */
    public boolean isRunning() { return running; }

    /** 获取 Screen 对象（供高级用法直接操作） */
    public Screen getScreen() { return screen; }

    /** 获取 GUI 对象 */
    public MultiWindowTextGUI getGui() { return gui; }

    /** 获取输入框组件 */
    public TextBox getInputTextBox() { return inputTextBox; }

    /** 获取只读组件视图 */
    public Map<String, TuiWidget> getWidgets() {
        return Collections.unmodifiableMap(widgets);
    }

    /** 获取组件数量 */
    public int getWidgetCount() { return widgets.size(); }
}
