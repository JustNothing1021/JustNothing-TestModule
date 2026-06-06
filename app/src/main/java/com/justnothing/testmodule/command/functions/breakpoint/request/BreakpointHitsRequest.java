package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("breakpoint:hits")
public class BreakpointHitsRequest extends CommandRequest {

    public BreakpointHitsRequest() {
        super();
    }
}
