package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.trace.TraceResult;

public class TraceExportRequest extends CommandRequest<TraceResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = "跟踪任务 ID",
        serializedName = "traceId"
    )
    private int traceId;

    @CmdParam(
        name = "filePath",
        position = 2,
        required = true,
        description = "导出文件路径",
        serializedName = "filePath"
    )
    private String filePath;

    public TraceExportRequest() {
        super();
    }

    public int getTraceId() { return traceId; }
    public void setTraceId(int id) { this.traceId = id; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
