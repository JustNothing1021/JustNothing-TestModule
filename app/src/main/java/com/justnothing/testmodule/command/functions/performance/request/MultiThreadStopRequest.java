package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.MultiThreadResult;

public class MultiThreadStopRequest extends PerformanceRequest<MultiThreadResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_MULTITHREAD_STOP_ID_DESC
    )
    private int taskId;

    public MultiThreadStopRequest() {
        super();
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
}
