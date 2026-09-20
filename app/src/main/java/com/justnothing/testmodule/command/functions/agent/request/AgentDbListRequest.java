package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.agent.AgentTexts;
import com.justnothing.testmodule.command.functions.agent.response.DbListResult;

public class AgentDbListRequest extends CommandRequest<DbListResult> {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = AgentTexts.PARAM_AGENT_DB_LIST_PKG_DESC)
    private String packageName;

    public AgentDbListRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
}
