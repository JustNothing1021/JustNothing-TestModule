package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("breakpoint:list")
public class BreakpointListRequest extends CommandRequest {

    public BreakpointListRequest() {
        super();
    }
}
