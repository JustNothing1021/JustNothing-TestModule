package com.justnothing.methodsclient.tui.widget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

/**
 * TUI 组件的创建/更新数据模型（信封模式）。
 * <p>
 * 通过 InteractiveProtocol 以 JSON 格式在服务端和客户端之间传输。
 * 服务端构造此对象并序列化为 JSON 字节，客户端反序列化后驱动 Lanterna 组件。
 * <p>
 * 结构：id + type + title + payload（类型化的配置对象，不再使用 Map）。
 * <p>
 * 使用示例（服务端）：
 * <pre>
 *   TuiWidgetData data = TuiWidgetData.createProgressBar("scan")
 *       .label("扫描类文件")
 *       .progress(75)
 *       .total(100);
 *   byte[] jsonBytes = data.toJsonBytes();
 * </pre>
 */
public class TuiWidgetData {

    @com.google.gson.annotations.Expose
    @com.google.gson.annotations.SerializedName("id")
    private String id;

    @com.google.gson.annotations.Expose
    @com.google.gson.annotations.SerializedName("type")
    private String type;

    @com.google.gson.annotations.Expose
    @com.google.gson.annotations.SerializedName("title")
    private String title;

    /** 类型化配置负载，具体类型由 widget type 决定 */
    @com.google.gson.annotations.Expose
    @com.google.gson.annotations.SerializedName("config")
    private TuiWidgetConfig payload;

    // ==================== 构造函数 ====================

    public TuiWidgetData() {}

    private TuiWidgetData(String id, TuiWidgetType widgetType) {
        this.id = id;
        this.type = widgetType.getName();
    }

    // ==================== 工厂方法 ====================

    /** 创建进度条组件数据（内部自动创建 ProgressBarConfig） */
    public static TuiWidgetData createProgressBar(String widgetId) {
        TuiWidgetData data = new TuiWidgetData(widgetId, TuiWidgetType.PROGRESS_BAR);
        data.payload = new ProgressBarConfig();
        return data;
    }

    /** 创建日志面板数据 */
    public static TuiWidgetData createLogPanel(String widgetId) {
        TuiWidgetData data = new TuiWidgetData(widgetId, TuiWidgetType.LOG_PANEL);
        data.payload = new LogPanelConfig();
        return data;
    }

    /** 创建状态栏数据 */
    public static TuiWidgetData createStatusBar(String widgetId) {
        TuiWidgetData data = new TuiWidgetData(widgetId, TuiWidgetType.STATUS_BAR);
        data.payload = new StatusBarConfig();
        return data;
    }

    /** 创建表格数据 */
    public static TuiWidgetData createTable(String widgetId) {
        TuiWidgetData data = new TuiWidgetData(widgetId, TuiWidgetType.TABLE);
        data.payload = new TableConfig();
        return data;
    }

    /** 创建 Spinner 数据 */
    public static TuiWidgetData createSpinner(String widgetId) {
        TuiWidgetData data = new TuiWidgetData(widgetId, TuiWidgetType.SPINNER);
        data.payload = new SpinnerConfig();
        return data;
    }

    // ==================== 链式配置 API — 通用 ====================

    public TuiWidgetData title(String title) { this.title = title; return this; }

    /**
     * 直接设置类型化配置对象。
     * 用于服务端构建完整配置后一次性赋值。
     */
    public TuiWidgetData setPayload(TuiWidgetConfig config) { this.payload = config; return this; }

    // ==================== 进度条专用快捷方法 ====================

    public TuiWidgetData progress(int value) {
        ensurePayload(ProgressBarConfig.class).setProgress(value);
        return this;
    }

    public TuiWidgetData total(int value) {
        ensurePayload(ProgressBarConfig.class).setTotal(value);
        return this;
    }

    public TuiWidgetData label(String text) {
        ensurePayload(ProgressBarConfig.class).setLabel(text);
        return this;
    }

    public TuiWidgetData color(String colorName) {
        ensurePayload(ProgressBarConfig.class).setColor(colorName);
        return this;
    }

    public TuiWidgetData showPercent(boolean show) {
        ensurePayload(ProgressBarConfig.class).setShowPercent(show);
        return this;
    }

    public TuiWidgetData speed(double itemsPerSec) {
        ensurePayload(ProgressBarConfig.class).setSpeed(itemsPerSec);
        return this;
    }

    public TuiWidgetData eta(long seconds) {
        ensurePayload(ProgressBarConfig.class).setEta(seconds);
        return this;
    }

    // ==================== 日志面板专用快捷方法 ====================

    public TuiWidgetData appendLine(String line) {
        ensurePayload(LogPanelConfig.class).setAppend(line);
        return this;
    }

    public TuiWidgetData appendLines(String[] lines) {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String s : lines) list.add(s);
        ensurePayload(LogPanelConfig.class).setAppendLines(list);
        return this;
    }

    public TuiWidgetData clear() {
        ensurePayload(LogPanelConfig.class).setClear(true);
        return this;
    }

    public TuiWidgetData lineColor(String color) {
        ensurePayload(LogPanelConfig.class).setLineColor(color);
        return this;
    }

    // ==================== 状态栏专用快捷方法 ====================

    public TuiWidgetData status(String text) {
        ensurePayload(StatusBarConfig.class).setStatus(text);
        return this;
    }

    public TuiWidgetData statusType(String typeStr) {
        ensurePayload(StatusBarConfig.class).setStatusType(typeStr);
        return this;
    }

    // ==================== 表格专用快捷方法 ====================

    public TuiWidgetData headers(String... headerValues) {
        ensurePayload(TableConfig.class).setHeaders(headerValues);
        return this;
    }

    public TuiWidgetData addRow(Object... values) {
        ensurePayload(TableConfig.class).setAddRow(values);
        return this;
    }

    public TuiWidgetData clearRows() {
        ensurePayload(TableConfig.class).setClearRows(true);
        return this;
    }

    // ==================== Spinner 专用快捷方法 ====================

    public TuiWidgetData spinnerText(String text) {
        ensurePayload(SpinnerConfig.class).setText(text);
        return this;
    }

    // ==================== 访问器 ====================

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }

    /**
     * 获取类型化配置负载。
     * 调用方可安全强转为具体的 Config 子类。
     */
    public TuiWidgetConfig getPayload() { return payload; }

    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.fromName(type);
    }

    /**
     * 便捷方法：获取 ProgressBarConfig（仅当 type 为 PROGRESS_BAR 时有效）
     */
    public ProgressBarConfig asProgressBarConfig() {
        return (payload instanceof ProgressBarConfig) ? (ProgressBarConfig) payload : null;
    }

    /** 便捷方法：获取 LogPanelConfig */
    public LogPanelConfig asLogPanelConfig() {
        return (payload instanceof LogPanelConfig) ? (LogPanelConfig) payload : null;
    }

    /** 便捷方法：获取 StatusBarConfig */
    public StatusBarConfig asStatusBarConfig() {
        return (payload instanceof StatusBarConfig) ? (StatusBarConfig) payload : null;
    }

    /** 便捷方法：获取 TableConfig */
    public TableConfig asTableConfig() {
        return (payload instanceof TableConfig) ? (TableConfig) payload : null;
    }

    /** 便捷方法：获取 SpinnerConfig */
    public SpinnerConfig asSpinnerConfig() {
        return (payload instanceof SpinnerConfig) ? (SpinnerConfig) payload : null;
    }

    // ==================== 序列化 / 反序列化 ====================

    /**
     * 序列化为 JSON 字节数组（用于协议传输）。
     * <p>
     * 使用项目统一的 GsonFactory，支持 @Expose 注解驱动的字段选择。
     */
    public byte[] toJsonBytes() {
        Gson gson = createTuiGson();
        String json = gson.toJson(this);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从 JSON 字节数组反序列化。
     * <p>
     * 根据 JSON 中的 {@code type} 字段自动将 {@code config} 反序列化为
     * 对应的具体配置子类（ProgressBarConfig / LogPanelConfig 等），
     * 实现多态反序列化。
     */
    public static TuiWidgetData fromJsonBytes(byte[] data) {
        if (data == null || data.length == 0) return null;

        String json = new String(data, StandardCharsets.UTF_8);
        Gson gson = createTuiGson();

        // 两阶段解析：
        // 1. 先解析为 JsonObject 以读取 type 字段
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) return null;
        JsonObject rootObj = root.getAsJsonObject();

        // 2. 反序列化信封本身（不含 config 多态字段）
        TuiWidgetData result = gson.fromJson(rootObj, TuiWidgetData.class);

        // 3. 根据类型将 config 反序列化为具体子类
        if (rootObj.has("config") && !rootObj.get("config").isJsonNull()) {
            String typeName = result.type;
            TuiWidgetType widgetType = TuiWidgetType.fromName(typeName);
            if (widgetType != null) {
                JsonObject configObj = rootObj.getAsJsonObject("config");
                result.payload = deserializeConfig(widgetType, configObj, gson);
            }
        }

        return result;
    }

    // ==================== 内部方法 ====================

    /**
     * 确保 payload 为指定类型，若不匹配则抛出异常。
     */
    @SuppressWarnings("unchecked")
    private <T extends TuiWidgetConfig> T ensurePayload(Class<T> expectedType) {
        if (payload == null || !expectedType.isInstance(payload)) {
            throw new IllegalStateException(
                    "当前 payload 类型不匹配: 期望 " + expectedType.getSimpleName()
                            + ", 实际 " + (payload != null ? payload.getClass().getSimpleName() : "null"));
        }
        return (T) payload;
    }

    /**
     * 根据组件类型将 config JsonObject 反序列化为对应的具体配置类。
     */
    private static TuiWidgetConfig deserializeConfig(TuiWidgetType type, JsonObject configObj, Gson gson) {
        switch (type.getCode()) {
            case 1: return gson.fromJson(configObj, ProgressBarConfig.class);
            case 2: return gson.fromJson(configObj, LogPanelConfig.class);
            case 3: return gson.fromJson(configObj, StatusBarConfig.class);
            case 4: return gson.fromJson(configObj, TableConfig.class);
            case 5: return gson.fromJson(configObj, SpinnerConfig.class);
            default: return null;
        }
    }

    /**
     * 创建专用于 TUI 数据的 Gson 实例。
     * <p>
     * 与项目默认 GsonFactory 不同，此处<strong>不禁用</strong>未标注 @Expose 的字段，
     * 因为 TuiWidgetData 及其配置类需要完整序列化所有业务字段。
     */
    private static Gson createTuiGson() {
        return new GsonBuilder()
                .serializeNulls()
                .create();
    }

    @Override
    public String toString() {
        return "TuiWidgetData{id='" + id + "', type=" + type
                + ", title='" + title + "', payload=" +
                (payload != null ? payload.getClass().getSimpleName() : "null") + "}";
    }
}
