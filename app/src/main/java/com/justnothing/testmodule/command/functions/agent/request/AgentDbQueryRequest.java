package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AgentDbQuery")
public class AgentDbQueryRequest extends CommandRequest {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = "目标应用包名")
    private String packageName;
    @Expose @SerializedName("dbName")
    @CmdParam(name = "db", position = 2, description = "数据库名称")
    private String dbName;
    @Expose @SerializedName("sql")
    @CmdParam(name = "sql", position = 3, description = "SQL 查询语句")
    private String sql;
    @Expose @SerializedName("limit")
    @CmdParam(name = "limit", required = false, description = "结果行数限制")
    private int limit;

    public AgentDbQueryRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
