package com.justnothing.testmodule.command.tui;

/**
 * TUI Widget 类型枚举（用于 Gson 多态反序列化）
 */
public enum TuiWidgetType {
    PROGRESS_BAR(1, "progress_bar"),
    SPINNER(2, "spinner"),
    LOG_PANEL(3, "log_panel"),
    STATUS_BAR(4, "status_bar"),
    TABLE(5, "table");

    private final int code;
    private final String name;

    TuiWidgetType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() { return code; }
    public String getName() { return name; }

    public static TuiWidgetType fromCode(int code) {
        for (TuiWidgetType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown TuiWidgetType code: " + code);
    }
}
