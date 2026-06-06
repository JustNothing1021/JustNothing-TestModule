package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("breakpoint:enable")
public class BreakpointEnableRequest extends CommandRequest {

    @CmdParam(name = "id", position = 1, required = true, description = "断点ID")
    private String id;

    public BreakpointEnableRequest() {
        super();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
