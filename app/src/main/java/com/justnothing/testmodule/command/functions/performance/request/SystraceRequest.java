package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.validator.AllowedValues;
import com.justnothing.testmodule.command.base.validator.Range;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("Systrace")
@SubCommand("systrace")
@AutoSerializable
public class SystraceRequest extends PerformanceRequest {

    @AllowedValues({"start", "stop", "report", "export"})
    @PositionalParam(name = "action", order = 1, required = true)
    private String action;

    @Range(min = 100, max = 600000)
    @PositionalParam(name = "duration", order = 2, required = false)
    private Integer duration;

    @PositionalParam(name = "categories", order = 3, required = false)
    private String categories;

    private Integer taskId;
    private String filePath;

    public SystraceRequest() {}
    public SystraceRequest(String action) { this.action = action; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("action", action);
        if (taskId != null) obj.put("taskId", taskId);
        if (duration != null) obj.put("duration", duration);
        if (categories != null) obj.put("categories", categories);
        if (filePath != null) obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public SystraceRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setAction(obj.optString("action", ""));
        if (obj.has("taskId")) setTaskId(obj.getInt("taskId"));
        if (obj.has("duration")) setDuration(obj.getInt("duration"));
        setCategories(obj.optString("categories", null));
        setFilePath(obj.optString("filePath", null));
        return this;
    }

    @Override
    public SystraceRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(SystraceRequest.class, args);
    }
}
