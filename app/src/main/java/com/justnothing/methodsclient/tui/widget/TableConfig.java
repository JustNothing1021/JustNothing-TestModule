package com.justnothing.methodsclient.tui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格组件配置。
 * <p>
 * 支持设置表头、添加数据行、清空行。
 */
public class TableConfig implements TuiWidgetConfig {

    /** 表头数组 */
    @Expose @SerializedName("headers")
    private String[] headers = new String[0];

    /** 待添加的数据行（UPDATE 时使用） */
    @Expose @SerializedName("addRow")
    private Object[] addRow = null;

    /** 清空所有行标志 */
    @Expose @SerializedName("clearRows")
    private boolean clearRows = false;

    // Gson 无参构造
    public TableConfig() {}

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.TABLE;
    }

    // ==================== Getter / Setter ====================

    public String[] getHeaders() { return headers; }
    public void setHeaders(String[] headers) { this.headers = headers != null ? headers : new String[0]; }

    public Object[] getAddRow() { return addRow; }
    public void setAddRow(Object[] addRow) { this.addRow = addRow; }

    public boolean isClearRows() { return clearRows; }
    public void setClearRows(boolean clearRows) { this.clearRows = clearRows; }

    /**
     * 工厂方法：设置表头
     */
    public static TableConfig withHeaders(String... h) {
        TableConfig cfg = new TableConfig();
        cfg.headers = h;
        return cfg;
    }

    /**
     * 工厂方法：添加一行
     */
    public static TableConfig withRow(Object... values) {
        TableConfig cfg = new TableConfig();
        cfg.addRow = values;
        return cfg;
    }
}
