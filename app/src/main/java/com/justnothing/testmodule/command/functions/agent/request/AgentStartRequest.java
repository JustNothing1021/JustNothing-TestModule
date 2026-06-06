package com.justnothing.testmodule.command.functions.agent.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AgentStart")
public class AgentStartRequest extends CommandRequest {
    @Expose @SerializedName("packageName")
    @CmdParam(name = "pkg", position = 1, description = "目标应用包名")
    private String packageName;

    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }
}
