package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class GetFieldValueRequest extends CommandRequest {
    
    private String className;
    private String fieldName;
    private String targetInstance;
    private boolean isStatic;
    
    public GetFieldValueRequest() {
        super();
    }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("requestId", getRequestId());
        obj.put("commandType", getCommandType());
        obj.put("className", className);
        obj.put("fieldName", fieldName);
        obj.put("targetInstance", targetInstance);
        obj.put("isStatic", isStatic);
        return obj;
    }
    
    @Override
    public GetFieldValueRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setFieldName(obj.optString("fieldName"));
        setTargetInstance(obj.optString("targetInstance", null));
        setStatic(obj.optBoolean("isStatic", false));
        return this;
    }
}
