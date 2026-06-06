package com.justnothing.methodsclient.tui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志面板组件配置。
 * <p>
 * 支持追加单行/多行日志、清空、设置颜色和最大行数。
 */
public class LogPanelConfig implements TuiWidgetConfig {

    /** 追加一行日志（UPDATE 时使用，CREATE 时通常为 null） */
    @Expose @SerializedName("append")
    private String append = null;

    /** 追加多行日志 */
    @Expose @SerializedName("appendLines")
    private List<String> appendLines = null;

    /** 清空所有日志 */
    @Expose @SerializedName("clear")
    private boolean clear = false;

    /** 日志颜色 ("red"/"green"/"blue"/"yellow"/"cyan"/null=默认) */
    @Expose @SerializedName("lineColor")
    private String lineColor = null;

    /** 最大保留行数 */
    @Expose @SerializedName("maxLines")
    private int maxLines = 100;

    // Gson 无参构造
    public LogPanelConfig() {}

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.LOG_PANEL;
    }

    // ==================== Getter / Setter ====================

    public String getAppend() { return append; }
    public void setAppend(String append) { this.append = append; }

    public List<String> getAppendLines() { return appendLines; }
    public void setAppendLines(List<String> appendLines) { this.appendLines = appendLines; }

    public boolean isClear() { return clear; }
    public void setClear(boolean clear) { this.clear = clear; }

    public String getLineColor() { return lineColor; }
    public void setLineColor(String lineColor) { this.lineColor = lineColor; }

    public int getMaxLines() { return maxLines; }
    public void setMaxLines(int maxLines) { this.maxLines = Math.max(maxLines, 1); }

    /**
     * 工厂方法：创建追加单行的配置
     */
    public static LogPanelConfig ofAppend(String line) {
        LogPanelConfig cfg = new LogPanelConfig();
        cfg.append = line;
        return cfg;
    }

    /**
     * 工厂方法：创建清空配置
     */
    public static LogPanelConfig ofClear() {
        LogPanelConfig cfg = new LogPanelConfig();
        cfg.clear = true;
        return cfg;
    }
}
