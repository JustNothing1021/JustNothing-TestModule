package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.SampleResult;

public class SampleReportRequest extends PerformanceRequest<SampleResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_SAMPLE_REPORT_ID_DESC
    )
    private Integer taskId;

    public SampleReportRequest() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
}
