package com.justnothing.methodsclient.tui.widget;

/**
 * TUI 组件配置基接口。
 * <p>
 * 所有组件的配置数据都实现此接口，确保类型安全和可序列化。
 * 通过 {@link com.google.gson.Gson} 序列化后可通过 InteractiveProtocol 在
 * 服务端和客户端之间传输，也便于未来扩展云端同步。
 */
public interface TuiWidgetConfig {
    /** 获取此配置对应的组件类型 */
    TuiWidgetType getWidgetType();
}
