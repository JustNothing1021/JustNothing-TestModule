package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.validator.AllowedValues;
import com.justnothing.testmodule.command.base.validator.Pattern;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("PerfHook")
@SubCommand("hook")
@AutoSerializable
public class PerfHookRequest extends PerformanceRequest {

    @AllowedValues({"start", "stop", "report", "export"})
    @PositionalParam(name = "action", order = 1)
    private String action;

    @Pattern(regex = "^[a-zA-Z_$][a-zA-Z0-9_$.]*$", description = "Java类名")
    @PositionalParam(name = "className", order = 2, required = false)
    private String className;

    @PositionalParam(name = "methodName", order = 3, required = false)
    private String methodName;

    @PositionalParam(name = "signature", order = 4, required = false, varArgs = true)
    private String signature;

    private Integer taskId;
    private String filePath;

    public PerfHookRequest() {}
    public PerfHookRequest(String action) { this.action = action; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("action", action).put("className", className).put("methodName", methodName)
                .put("signature", signature);
        if (taskId != null) obj.put("taskId", taskId);
        if (filePath != null) obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public PerfHookRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setAction(obj.optString("action", ""));
        setClassName(obj.optString("className", ""));
        setMethodName(obj.optString("methodName", null));
        setSignature(obj.optString("signature", null));
        if (obj.has("taskId")) setTaskId(obj.getInt("taskId"));
        setFilePath(obj.optString("filePath", null));
        return this;
    }

    @Override
    public PerfHookRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(PerfHookRequest.class, args);
    }
}
