package com.justnothing.methodsclient.tui.widget;

/**
 * TUI 组件类型枚举。
 * <p>
 * 每种类型对应一种可由服务端通过 InteractiveProtocol 驱动的终端 UI 组件。
 * 客户端根据此类型创建对应的 Lanterna 组件实例。
 */
public enum TuiWidgetType {

    /** 动画进度条（带百分比/速度/ETA） */
    PROGRESS_BAR(1, "progress_bar"),

    /** 滚动日志面板（追加式文本区域） */
    LOG_PANEL(2, "log_panel"),

    /** 单行状态指示器（带颜色标签） */
    STATUS_BAR(3, "status_bar"),

    /** 结构化数据表格（列对齐、带表头） */
    TABLE(4, "table"),

    /** 不确定进度动画（旋转 spinner） */
    SPINNER(5, "spinner");

    private final int code;
    private final String name;

    TuiWidgetType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() { return code; }
    public String getName() { return name; }

    /**
     * 根据协议中的数字代码查找组件类型
     */
    public static TuiWidgetType fromCode(int code) {
        for (TuiWidgetType type : values()) {
            if (type.code == code) return type;
        }
        return null;
    }

    /**
     * 根据名称查找（JSON 编码时使用）
     */
    public static TuiWidgetType fromName(String name) {
        for (TuiWidgetType type : values()) {
            if (type.name.equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}
