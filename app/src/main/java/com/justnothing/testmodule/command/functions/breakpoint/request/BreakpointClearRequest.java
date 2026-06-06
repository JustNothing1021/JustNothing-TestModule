package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("breakpoint:clear")
public class BreakpointClearRequest extends CommandRequest {

    public BreakpointClearRequest() {
        super();
    }
}
