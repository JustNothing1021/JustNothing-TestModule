package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:hierarchical:report")
public class HierarchicalReportRequest extends PerformanceRequest {

    @CmdParam(
        name = "id",
        position = 1,
        required = false,
        description = "任务 ID（可选，默认显示最新）"
    )
    private Integer taskId;

    public HierarchicalReportRequest() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (taskId != null) obj.put("taskId", taskId);
        return obj;
    }

    @Override
    public HierarchicalReportRequest fromJson(JSONObject obj) throws org.json.JSONException {
        setRequestId(obj.optString("requestId"));
        if (obj.has("taskId")) setTaskId(obj.getInt("taskId"));
        return this;
    }
}
