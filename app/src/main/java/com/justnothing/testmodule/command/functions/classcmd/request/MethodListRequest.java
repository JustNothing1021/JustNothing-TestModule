package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class MethodListRequest extends ClassCommandRequest {

    private String className;
    private boolean verbose;

    public MethodListRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("verbose", verbose);
        return obj;
    }

    @Override
    public MethodListRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setVerbose(obj.optBoolean("verbose", false));
        return this;
    }

    @Override
    public MethodListRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("参数不足: class list [options] <class>");
        }

        int startIdx = 0;
        if ("-v".equals(args[0]) || "--verbose".equals(args[0])) {
            verbose = true;
            startIdx = 1;
        }

        if (args.length <= startIdx) {
            throw new IllegalCommandLineArgumentException("详细模式需要指定类名: class list -v <class>");
        }

        className = args[args.length - 1];
        return this;
    }
}
