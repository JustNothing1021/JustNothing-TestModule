package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.PerfTraceResult;

public class TraceStopRequest extends PerformanceRequest<PerfTraceResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_TRACE_STOP_ID_DESC
    )
    private int taskId;

    public TraceStopRequest() {
        super();
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
}
