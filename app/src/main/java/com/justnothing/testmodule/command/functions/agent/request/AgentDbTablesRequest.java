package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AgentDbTables")
public class AgentDbTablesRequest extends CommandRequest {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = "目标应用包名")
    private String packageName;
    @Expose @SerializedName("dbName")
    @CmdParam(name = "db", position = 2, description = "数据库名称")
    private String dbName;

    public AgentDbTablesRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
}
