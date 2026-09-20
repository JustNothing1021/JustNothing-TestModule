package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.agent.AgentTexts;
import com.justnothing.testmodule.command.functions.agent.response.DbTablesResult;

public class AgentDbTablesRequest extends CommandRequest<DbTablesResult> {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = AgentTexts.PARAM_AGENT_DB_TABLES_PKG_DESC)
    private String packageName;
    @Expose @SerializedName("dbName")
    @CmdParam(name = "db", position = 2, description = AgentTexts.PARAM_AGENT_DB_TABLES_DB_DESC)
    private String dbName;

    public AgentDbTablesRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
}
