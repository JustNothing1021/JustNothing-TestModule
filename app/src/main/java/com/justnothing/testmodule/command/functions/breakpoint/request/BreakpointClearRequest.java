package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("breakpoint:clear")
public class BreakpointClearRequest extends CommandRequest {

    public BreakpointClearRequest() {
        super();
    }
}
