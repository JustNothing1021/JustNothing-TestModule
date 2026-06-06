package com.justnothing.methodsclient.tui.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.Component;

/**
 * TUI 组件接口。
 * <p>
 * 所有可由服务端驱动的终端 UI 组件都实现此接口。
 * TuiManager 通过此接口统一管理组件的生命周期和绘制。
 */
public interface TuiWidget {

    /**
     * 获取组件唯一 ID（对应协议中的 widgetId）
     */
    String getWidgetId();

    /**
     * 获取组件类型
     */
    TuiWidgetType getWidgetType();

    /**
     * 获取组件标题（用于面板头部显示）
     */
    String getTitle();

    /**
     * 获取 Lanterna GUI Component 实例
     * <p>
     * 返回的 Component 会被添加到 Lanterna Panel 或 Window 中进行布局管理。
     * 如果组件使用自定义绘制（不依赖 Lanterna 的 Panel 布局），返回 null 并重写 {@link #draw} 方法。
     */
    Component getComponent();

    /**
     * 使用服务端数据更新组件状态。
     *
     * @param data 来自 InteractiveProtocol 的更新数据
     */
    void update(TuiWidgetData data);

    /**
     * 自定义绘制方法（可选）。
     * <p>
     * 如果 getComponent() 返回非 null，Lanterna 的布局系统会自动处理绘制，
     * 此方法不会被调用。只有自定义绘制的组件需要实现此方法。
     *
     * @param g      TextGraphics 绘制上下文
     * @param origin 组件左上角位置（相对于屏幕）
     * @param size   组件可用大小
     */
    default void draw(TextGraphics g, TerminalPosition origin, TerminalSize size) {
        // 默认空实现：使用 Lanterna Component 系统
    }

    /**
     * 销毁组件，释放资源
     */
    void dispose();

    /**
     * 组件是否需要持续刷新（如动画进度条、spinner）
     */
    default boolean needsRefresh() { return false; }

    /**
     * 获取建议的最小尺寸
     */
    default TerminalSize getPreferredSize() { return new TerminalSize(20, 3); }
}
