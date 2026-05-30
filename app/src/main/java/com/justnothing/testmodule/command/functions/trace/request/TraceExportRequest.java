package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("trace:export")
public class TraceExportRequest extends CommandRequest {

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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("traceId", traceId);
        if (filePath != null) obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public TraceExportRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setTraceId(obj.optInt("traceId", 0));
        setFilePath(obj.optString("filePath", null));
        return this;
    }
}
