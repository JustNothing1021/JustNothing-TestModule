package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.validator.AllowedValues;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("Trace")
@SubCommand("trace")
@AutoSerializable
public class TraceRequest extends PerformanceRequest {

    @AllowedValues({"start", "stop", "report", "export"})
    @PositionalParam(name = "action", order = 1)
    private String action;

    private Integer taskId;
    private String filePath;

    public TraceRequest() {}
    public TraceRequest(String action) { this.action = action; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("action", action);
        if (taskId != null) obj.put("taskId", taskId);
        if (filePath != null) obj.put("filePath", filePath);
        return obj;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TraceRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setAction(obj.optString("action", ""));
        if (obj.has("taskId")) setTaskId(obj.getInt("taskId"));
        setFilePath(obj.optString("filePath", null));
        return this;
    }

    @Override
    public TraceRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(TraceRequest.class, args);
    }
}
