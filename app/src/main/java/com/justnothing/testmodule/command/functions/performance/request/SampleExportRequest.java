package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.SampleResult;

public class SampleExportRequest extends PerformanceRequest<SampleResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_SAMPLE_EXPORT_ID_DESC
    )
    private int taskId;

    @CmdParam(
        name = "filePath",
        position = 2,
        required = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_SAMPLE_EXPORT_FILEPATH_DESC
    )
    private String filePath;

    public SampleExportRequest() {
        super();
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
