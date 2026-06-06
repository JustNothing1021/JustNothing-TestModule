package com.justnothing.testmodule.command.tui.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * 旋转指示器 Widget 配置（Gson 序列化 DTO）
 */
public class SpinnerConfig {

    @Expose @SerializedName("text")
    private String text;

    @Expose @SerializedName("frameStyle")
    private String frameStyle = "dots";

    @Expose @SerializedName("intervalMs")
    private int intervalMs = 80;

    @Expose @SerializedName("color")
    private int color = 36; // JLine AttributedStyle.CYAN

    @Expose @SerializedName("dynamicText")
    private String dynamicText;

    public SpinnerConfig() {
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getFrameStyle() { return frameStyle; }
    public void setFrameStyle(String frameStyle) { this.frameStyle = frameStyle; }

    public int getIntervalMs() { return intervalMs; }
    public void setIntervalMs(int intervalMs) { this.intervalMs = intervalMs; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public String getDynamicText() { return dynamicText; }
    public void setDynamicText(String dynamicText) { this.dynamicText = dynamicText; }
}
