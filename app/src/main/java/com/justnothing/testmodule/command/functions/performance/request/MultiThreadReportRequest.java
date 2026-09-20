package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.MultiThreadResult;

public class MultiThreadReportRequest extends PerformanceRequest<MultiThreadResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_MULTITHREAD_REPORT_ID_DESC
    )
    private Integer taskId;

    public MultiThreadReportRequest() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
}
