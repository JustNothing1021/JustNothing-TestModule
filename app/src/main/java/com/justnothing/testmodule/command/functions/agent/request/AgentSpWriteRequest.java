package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.agent.AgentTexts;
import com.justnothing.testmodule.command.functions.agent.response.SpWriteResult;

public class AgentSpWriteRequest extends CommandRequest<SpWriteResult> {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = AgentTexts.PARAM_AGENT_SP_WRITE_PKG_DESC)
    private String packageName;
    @Expose @SerializedName("spName")
    @CmdParam(name = "name", position = 2, description = AgentTexts.PARAM_AGENT_SP_WRITE_NAME_DESC)
    private String spName;
    @Expose @SerializedName("key")
    @CmdParam(name = "key", position = 3, description = AgentTexts.PARAM_AGENT_SP_WRITE_KEY_DESC)
    private String key;
    @Expose @SerializedName("value")
    @CmdParam(name = "value", position = 4, description = AgentTexts.PARAM_AGENT_SP_WRITE_VALUE_DESC)
    private String value;
    @Expose @SerializedName("valueType")
    @CmdParam(name = "type", position = 5, required = false, description = AgentTexts.PARAM_AGENT_SP_WRITE_TYPE_DESC)
    private int valueType;

    public AgentSpWriteRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getSpName() { return spName; }
    public void setSpName(String spName) { this.spName = spName; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getValueType() { return valueType; }
    public void setValueType(int valueType) { this.valueType = valueType; }
}
