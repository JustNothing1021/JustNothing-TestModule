package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.agent.AgentTexts;
import com.justnothing.testmodule.command.functions.agent.response.DbQueryResult;

public class AgentDbQueryRequest extends CommandRequest<DbQueryResult> {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = AgentTexts.PARAM_AGENT_DB_QUERY_PKG_DESC)
    private String packageName;
    @Expose @SerializedName("dbName")
    @CmdParam(name = "db", position = 2, description = AgentTexts.PARAM_AGENT_DB_QUERY_DB_DESC)
    private String dbName;
    @Expose @SerializedName("sql")
    @CmdParam(name = "sql", position = 3, description = AgentTexts.PARAM_AGENT_DB_QUERY_SQL_DESC)
    private String sql;
    @Expose @SerializedName("limit")
    @CmdParam(name = "limit", required = false, description = AgentTexts.PARAM_AGENT_DB_QUERY_LIMIT_DESC)
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
