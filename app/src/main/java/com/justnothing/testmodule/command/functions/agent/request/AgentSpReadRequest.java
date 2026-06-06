package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AgentSpRead")
public class AgentSpReadRequest extends CommandRequest {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = "目标应用包名")
    private String packageName;
    @Expose @SerializedName("spName")
    @CmdParam(name = "name", position = 2, description = "SharedPreferences 名称")
    private String spName;
    @Expose @SerializedName("keyFilter")
    @CmdParam(name = "key", position = 3, required = false, description = "键名过滤（可选）")
    private String keyFilter;

    public AgentSpReadRequest() {}

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getSpName() { return spName; }
    public void setSpName(String spName) { this.spName = spName; }

    public String getKeyFilter() { return keyFilter; }
    public void setKeyFilter(String keyFilter) { this.keyFilter = keyFilter; }
}
