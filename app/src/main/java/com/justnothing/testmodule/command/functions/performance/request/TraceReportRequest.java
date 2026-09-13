package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.response.TraceResult;

public class TraceReportRequest extends PerformanceRequest<TraceResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = false,
        description = "任务 ID（可选，默认显示最新）"
    )
    private Integer taskId;

    public TraceReportRequest() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
}
