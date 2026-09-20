package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.trace.response.TraceResult;
import com.justnothing.testmodule.command.functions.trace.TraceTexts;

public class TraceExportRequest extends CommandRequest<TraceResult> {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = TraceTexts.PARAM_TRACE_EXPORT_ID_DESC,
        serializedName = "traceId"
    )
    private int traceId;

    @CmdParam(
        name = "filePath",
        position = 2,
        required = true,
        description = TraceTexts.PARAM_TRACE_EXPORT_FILEPATH_DESC,
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
