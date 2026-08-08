package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("breakpoint:hits")
public class BreakpointHitsRequest extends CommandRequest {

    public BreakpointHitsRequest() {
        super();
    }
}
