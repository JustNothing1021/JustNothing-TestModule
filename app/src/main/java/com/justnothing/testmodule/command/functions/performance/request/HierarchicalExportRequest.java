package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:hierarchical:export")
public class HierarchicalExportRequest extends PerformanceRequest {

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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("taskId", taskId);
        if (filePath != null) obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public HierarchicalExportRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setTaskId(obj.optInt("taskId", 0));
        setFilePath(obj.optString("filePath", null));
        return this;
    }
}
