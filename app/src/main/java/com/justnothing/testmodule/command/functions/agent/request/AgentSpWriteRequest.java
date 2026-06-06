package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AgentSpWrite")
public class AgentSpWriteRequest extends CommandRequest {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = "目标应用包名")
    private String packageName;
    @Expose @SerializedName("spName")
    @CmdParam(name = "name", position = 2, description = "SharedPreferences 名称")
    private String spName;
    @Expose @SerializedName("key")
    @CmdParam(name = "key", position = 3, description = "键名")
    private String key;
    @Expose @SerializedName("value")
    @CmdParam(name = "value", position = 4, description = "值")
    private String value;
    @Expose @SerializedName("valueType")
    @CmdParam(name = "type", position = 5, required = false, description = "值类型（可选）")
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
