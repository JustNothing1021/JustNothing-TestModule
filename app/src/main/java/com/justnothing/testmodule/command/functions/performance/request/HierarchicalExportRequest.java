package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.response.HierarchicalResult;

public class HierarchicalExportRequest extends PerformanceRequest<HierarchicalResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = "任务 ID"
    )
    private int taskId;

    @CmdParam(
        name = "filePath",
        position = 2,
        required = true,
        description = "导出文件路径"
    )
    private String filePath;

    public HierarchicalExportRequest() {
        super();
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
