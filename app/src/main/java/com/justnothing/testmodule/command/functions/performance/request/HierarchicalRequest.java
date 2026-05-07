package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("HierarchicalSample")
@SubCommand("hierarchical")
@AutoSerializable
public class HierarchicalRequest extends PerformanceRequest {

    @PositionalParam(name = "action", order = 1, required = true)
    private String action;

    @PositionalParam(name = "taskId", order = 2, required = false)
    private Integer taskId;

    @PositionalParam(name = "sampleRate", order = 3, required = false)
    private Integer sampleRate;

    @PositionalParam(name = "filePath", order = 4, required = false)
    private String filePath;

    @KeywordParam(name = "exclude", names = {"-e"})
    private String exclude;

    public HierarchicalRequest() {}
    public HierarchicalRequest(String action) { this.action = action; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public Integer getSampleRate() { return sampleRate; }
    public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getExclude() { return exclude; }
    public void setExclude(String exclude) { this.exclude = exclude; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("action", action);
        if (taskId != null) obj.put("taskId", taskId);
        if (sampleRate != null) obj.put("sampleRate", sampleRate);
        if (filePath != null) obj.put("filePath", filePath);
        if (exclude != null) obj.put("exclude", exclude);
        return obj;
    }

    @Override
    public HierarchicalRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setAction(obj.optString("action", ""));
        if (obj.has("taskId")) setTaskId(obj.getInt("taskId"));
        if (obj.has("sampleRate")) setSampleRate(obj.getInt("sampleRate"));
        setFilePath(obj.optString("filePath", null));
        setExclude(obj.optString("exclude", null));
        return this;
    }

    @Override
    public HierarchicalRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(HierarchicalRequest.class, args);
    }
}
