package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.framework.base.command.CmdParam;

@SerializeKeyName("breakpoint:disable")
public class BreakpointDisableRequest extends CommandRequest {

    @CmdParam(name = "id", position = 1, required = true, description = "断点ID")
    private String id;

    public BreakpointDisableRequest() {
        super();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
