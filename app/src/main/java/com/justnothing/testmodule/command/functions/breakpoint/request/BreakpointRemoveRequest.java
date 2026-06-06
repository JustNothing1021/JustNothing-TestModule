package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("breakpoint:remove")
public class BreakpointRemoveRequest extends CommandRequest {

    @CmdParam(name = "id", position = 1, required = true, description = "断点ID")
    private String id;

    public BreakpointRemoveRequest() {
        super();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
