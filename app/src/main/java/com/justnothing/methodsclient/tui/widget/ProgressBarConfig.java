package com.justnothing.methodsclient.tui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * 进度条组件配置。
 * <p>
 * 包含进度值、总量、标签文本、颜色、速度/ETA 等字段。
 */
public class ProgressBarConfig implements TuiWidgetConfig {

    @Expose @SerializedName("progress")
    private int progress = 0;

    @Expose @SerializedName("total")
    private int total = 100;

    @Expose @SerializedName("label")
    private String label = "";

    @Expose @SerializedName("color")
    private String color = "cyan";

    @Expose @SerializedName("showPercent")
    private boolean showPercent = true;

    @Expose @SerializedName("speed")
    private double speed = 0.0;

    @Expose @SerializedName("eta")
    private long eta = 0L;

    // Gson 无参构造
    public ProgressBarConfig() {}

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.PROGRESS_BAR;
    }

    // ==================== Getter / Setter ====================

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label != null ? label : ""; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color != null ? color : "cyan"; }

    public boolean isShowPercent() { return showPercent; }
    public void setShowPercent(boolean showPercent) { this.showPercent = showPercent; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public long getEta() { return eta; }
    public void setEta(long eta) { this.eta = eta; }
}
