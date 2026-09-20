package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.trace.response.TraceResult;
import com.justnothing.testmodule.command.functions.trace.TraceTexts;

public class TraceShowRequest extends CommandRequest<TraceResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = TraceTexts.PARAM_TRACE_SHOW_ID_DESC,
        serializedName = "traceId"
    )
    private int traceId;

    public TraceShowRequest() {
        super();
    }

    public int getTraceId() { return traceId; }
    public void setTraceId(int id) { this.traceId = id; }
}
