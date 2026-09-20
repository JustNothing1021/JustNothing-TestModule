package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointTexts;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;

public class BreakpointRemoveRequest extends CommandRequest<BreakpointResult> {

    @CmdParam(name = "id", position = 1, required = true, description = BreakpointTexts.PARAM_BREAKPOINT_REMOVE_ID_DESC)
    private String id;

    public BreakpointRemoveRequest() {
        super();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
