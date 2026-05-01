package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class ClassHierarchyRequest extends ClassCommandRequest {
    
    private String className;
    
    public ClassHierarchyRequest() {
        super();
    }
    
    public ClassHierarchyRequest(String className) {
        super();
        this.className = className;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        return obj;
    }
    
    @Override
    public ClassHierarchyRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        this.setClassName(obj.optString("className"));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) {
            className = args[0];
        }
        return this;
    }
}
