package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("network:record")
public class NetworkRecordRequest extends CommandRequest {

    @CmdParam(name = "enable", required = false, description = "开启或关闭记录")
    private Boolean enable;

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
