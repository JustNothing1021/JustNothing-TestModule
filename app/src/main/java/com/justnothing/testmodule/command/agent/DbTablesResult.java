package com.justnothing.testmodule.command.agent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("DbTablesResult")
@AutoSerializable
public class DbTablesResult extends CommandResult {

    @Expose @SerializedName("dbName")
    private String dbName;

    @Expose @SerializedName("tables")
    private List<String> tables;

    public DbTablesResult() {
        super();
        this.tables = new ArrayList<>();
    }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }

    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }
    public void addTable(String table) { this.tables.add(table); }
}
