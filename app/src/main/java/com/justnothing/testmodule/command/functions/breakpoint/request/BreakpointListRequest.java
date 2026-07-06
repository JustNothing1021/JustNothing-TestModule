package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("breakpoint:list")
public class BreakpointListRequest extends CommandRequest {

    public BreakpointListRequest() {
        super();
    }
}
