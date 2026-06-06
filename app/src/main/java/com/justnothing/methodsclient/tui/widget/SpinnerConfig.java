package com.justnothing.methodsclient.tui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Spinner 组件配置。
 * <p>
 * 支持设置显示文本和颜色。
 */
public class SpinnerConfig implements TuiWidgetConfig {

    /** 显示文本 */
    @Expose @SerializedName("text")
    private String text = "处理中...";

    /** 颜色名称 */
    @Expose @SerializedName("color")
    private String color = "cyan";

    // Gson 无参构造
    public SpinnerConfig() {}

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.SPINNER;
    }

    // ==================== Getter / Setter ====================

    public String getText() { return text; }
    public void setText(String text) { this.text = text != null ? text : "处理中..."; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color != null ? color : "cyan"; }

    /**
     * 工厂方法：快速构建 spinner 配置
     */
    public static SpinnerConfig of(String displayText, String colorName) {
        SpinnerConfig cfg = new SpinnerConfig();
        cfg.text = displayText;
        cfg.color = colorName;
        return cfg;
    }
}
