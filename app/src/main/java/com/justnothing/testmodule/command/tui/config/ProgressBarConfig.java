package com.justnothing.testmodule.command.tui.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * 进度条 Widget 配置（Gson 序列化 DTO）
 */
public class ProgressBarConfig {

    @Expose @SerializedName("taskName")
    private String taskName;

    @Expose @SerializedName("max")
    private long max = 100;

    @Expose @SerializedName("current")
    private long current = 0;

    @Expose @SerializedName("style")
    private String style = "unicode";

    @Expose @SerializedName("showPercent")
    private boolean showPercent = true;

    @Expose @SerializedName("showCount")
    private boolean showCount = false;

    @Expose @SerializedName("showEta")
    private boolean showEta = true;

    @Expose @SerializedName("showSpeed")
    private boolean showSpeed = false;

    @Expose @SerializedName("showElapsed")
    private boolean showElapsed = false;

    @Expose @SerializedName("text")
    private String text;

    @Expose @SerializedName("extraMessage")
    private String extraMessage;

    public ProgressBarConfig() {
    }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public long getMax() { return max; }
    public void setMax(long max) { this.max = max; }

    public long getCurrent() { return current; }
    public void setCurrent(long current) { this.current = current; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public boolean isShowPercent() { return showPercent; }
    public void setShowPercent(boolean showPercent) { this.showPercent = showPercent; }

    public boolean isShowCount() { return showCount; }
    public void setShowCount(boolean showCount) { this.showCount = showCount; }

    public boolean isShowEta() { return showEta; }
    public void setShowEta(boolean showEta) { this.showEta = showEta; }

    public boolean isShowSpeed() { return showSpeed; }
    public void setShowSpeed(boolean showSpeed) { this.showSpeed = showSpeed; }

    public boolean isShowElapsed() { return showElapsed; }
    public void setShowElapsed(boolean showElapsed) { this.showElapsed = showElapsed; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getExtraMessage() { return extraMessage; }
    public void setExtraMessage(String extraMessage) { this.extraMessage = extraMessage; }
}
