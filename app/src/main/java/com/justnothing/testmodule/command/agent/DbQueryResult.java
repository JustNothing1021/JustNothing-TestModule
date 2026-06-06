package com.justnothing.testmodule.command.agent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SerializeKeyName("DbQueryResult")
public class DbQueryResult extends CommandResult {

    @Expose @SerializedName("dbName")
    private String dbName;

    @Expose @SerializedName("columns")
    private List<String> columns;

    @Expose @SerializedName("rows")
    private List<Map<String, Object>> rows;

    @Expose @SerializedName("rowCount")
    private int rowCount;

    public DbQueryResult() {
        super();
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public void addRow(Map<String, Object> row) { this.rows.add(row); rowCount++; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
}
