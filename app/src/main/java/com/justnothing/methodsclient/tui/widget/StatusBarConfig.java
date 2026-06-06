package com.justnothing.methodsclient.tui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * 状态栏组件配置。
 * <p>
 * 包含状态文本和状态类型（success/error/warning/info/running）。
 */
public class StatusBarConfig implements TuiWidgetConfig {

    /** 状态文本 */
    @Expose @SerializedName("status")
    private String status = "";

    /** 状态类型: "success" / "error" / "warning" / "info" / "running" */
    @Expose @SerializedName("statusType")
    private String statusType = "info";

    // Gson 无参构造
    public StatusBarConfig() {}

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.STATUS_BAR;
    }

    // ==================== Getter / Setter ====================

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status != null ? status : ""; }

    public String getStatusType() { return statusType; }
    public void setStatusType(String statusType) {
        if (statusType == null) {
            this.statusType = "info";
            return;
        }
        String lower = statusType.toLowerCase();
        if ("success".equals(lower) || "error".equals(lower)
                || "warning".equals(lower) || "info".equals(lower)
                || "running".equals(lower)) {
            this.statusType = lower;
        } else {
            this.statusType = "info";
        }
    }

    /** 是否为 running 类型（需要持续刷新动画） */
    public boolean isRunning() {
        return "running".equals(statusType);
    }

    /**
     * 工厂方法：快速构建状态配置
     */
    public static StatusBarConfig of(String text, String type) {
        StatusBarConfig cfg = new StatusBarConfig();
        cfg.status = text;
        cfg.setStatusType(type);
        return cfg;
    }
}
