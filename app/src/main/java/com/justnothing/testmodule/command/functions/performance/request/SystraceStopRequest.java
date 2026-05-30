package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:systrace:stop")
public class SystraceStopRequest extends PerformanceRequest {

    @CmdParam(
        name = "id",
        position = 1,
        required = true,
        description = "任务 ID"
    )
    private int taskId;

    public SystraceStopRequest() {
        super();
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("taskId", taskId);
        return obj;
    }

    @Override
    public SystraceStopRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setTaskId(obj.optInt("taskId", 0));
        return this;
    }
}
