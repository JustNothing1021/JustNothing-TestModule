package com.justnothing.testmodule.command.tui;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.GsonFactory;

import java.nio.charset.StandardCharsets;

/**
 * TUI Widget 数据信封（Envelope）
 *
 * <p>作为 TYPE_TUI_WIDGET_CREATE / UPDATE 等消息的 JSON 载体，
 * 通过 socket 协议发送给客户端。payload 字段根据实际运行时类型
 * （ProgressBarConfig / SpinnerConfig 等）序列化为嵌套 JSON 对象。</p>
 *
 * <p>使用方式：</p>
 * <pre>
 *   // 创建进度条
 *   TuiWidgetData data = TuiWidgetData.create("bar-scan", TuiWidgetType.PROGRESS_BAR, "扫描中", barConfig);
 *   byte[] payload = data.toJsonBytes();
 *
 *   // 更新进度
 *   TuiWidgetData update = TuiWidgetData.update("bar-scan", TuiWidgetType.PROGRESS_BAR, updatedBarConfig);
 *
 *   // 销毁
 *   TuiWidgetData destroy = TuiWidgetData.destroy("bar-scan");
 *
 *   // 清空全部
 *   TuiWidgetData clearAll = TuiWidgetData.clearAll();
 * </pre>
 */
public class TuiWidgetData {

    @Expose @SerializedName("widgetId")
    private String widgetId;

    @Expose @SerializedName("type")
    private TuiWidgetType type;

    @Expose @SerializedName("title")
    private String title;

    @Expose @SerializedName("action")
    private String action;

    @Expose @SerializedName("payload")
    private Object payload;

    public TuiWidgetData() {
    }

    // ---- 工厂方法 ----

    /**
     * 创建 Widget
     */
    public static TuiWidgetData create(String widgetId, TuiWidgetType type, String title, Object config) {
        TuiWidgetData data = new TuiWidgetData();
        data.widgetId = widgetId;
        data.type = type;
        data.title = title;
        data.action = "create";
        data.payload = config;
        return data;
    }

    /**
     * 更新 Widget
     */
    public static TuiWidgetData update(String widgetId, TuiWidgetType type, Object config) {
        TuiWidgetData data = new TuiWidgetData();
        data.widgetId = widgetId;
        data.type = type;
        data.action = "update";
        data.payload = config;
        return data;
    }

    /**
     * 销毁指定 Widget
     */
    public static TuiWidgetData destroy(String widgetId) {
        TuiWidgetData data = new TuiWidgetData();
        data.widgetId = widgetId;
        data.action = "destroy";
        return data;
    }

    /**
     * 清空所有 Widget
     */
    public static TuiWidgetData clearAll() {
        TuiWidgetData data = new TuiWidgetData();
        data.action = "clear_all";
        return data;
    }

    // ---- 序列化 / 反序列化 ----

    /**
     * 序列化为 JSON 字节数组（UTF-8）
     */
    public byte[] toJsonBytes() {
        Gson gson = GsonFactory.getInstance();
        String json = gson.toJson(this);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从 JSON 字节数组反序列化
     */
    public static TuiWidgetData fromJsonBytes(byte[] data) {
        Gson gson = GsonFactory.getInstance();
        String json = new String(data, StandardCharsets.UTF_8);
        return gson.fromJson(json, TuiWidgetData.class);
    }

    // ---- Getter / Setter ----

    public String getWidgetId() { return widgetId; }
    public void setWidgetId(String widgetId) { this.widgetId = widgetId; }

    public TuiWidgetType getType() { return type; }
    public void setType(TuiWidgetType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}
