package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.PerfHookResult;

public class PerfHookReportRequest extends PerformanceRequest<PerfHookResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_HOOK_REPORT_ID_DESC
    )
    private Integer taskId;

    public PerfHookReportRequest() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
}
