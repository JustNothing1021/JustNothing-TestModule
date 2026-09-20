package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.agent.AgentTexts;
import com.justnothing.testmodule.command.functions.agent.response.SpReadResult;

public class AgentSpReadRequest extends CommandRequest<SpReadResult> {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = AgentTexts.PARAM_AGENT_SP_READ_PKG_DESC)
    private String packageName;
    @Expose @SerializedName("spName")
    @CmdParam(name = "name", position = 2, description = AgentTexts.PARAM_AGENT_SP_READ_NAME_DESC)
    private String spName;
    @Expose @SerializedName("keyFilter")
    @CmdParam(name = "key", position = 3, required = false, description = AgentTexts.PARAM_AGENT_SP_READ_KEY_DESC)
    private String keyFilter;

    public AgentSpReadRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getSpName() { return spName; }
    public void setSpName(String spName) { this.spName = spName; }

    public String getKeyFilter() { return keyFilter; }
    public void setKeyFilter(String keyFilter) { this.keyFilter = keyFilter; }
}
