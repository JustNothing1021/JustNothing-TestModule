package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONObject;

public class SetFieldValueRequest extends CommandRequest {
    
    private String className;
    private String fieldName;
    private String targetInstance;
    private String valueExpression;
    private String valueTypeHint;
    private boolean isStatic;
    
    public SetFieldValueRequest() {
        super();
    }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }
    public String getValueExpression() { return valueExpression; }
    public void setValueExpression(String valueExpression) { this.valueExpression = valueExpression; }
    public String getValueTypeHint() { return valueTypeHint; }
    public void setValueTypeHint(String valueTypeHint) { this.valueTypeHint = valueTypeHint; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    
    @Override
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = new JSONObject();
        obj.put("requestId", getRequestId());
        obj.put("commandType", getCommandType());
        obj.put("className", className);
        obj.put("fieldName", fieldName);
        obj.put("targetInstance", targetInstance);
        obj.put("valueExpression", valueExpression);
        obj.put("valueTypeHint", valueTypeHint);
        obj.put("isStatic", isStatic);
        return obj;
    }
    
    @Override
    public SetFieldValueRequest fromJson(JSONObject obj) throws org.json.JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setFieldName(obj.optString("fieldName"));
        setTargetInstance(obj.optString("targetInstance", null));
        setValueExpression(obj.optString("valueExpression"));
        setValueTypeHint(obj.optString("valueTypeHint", null));
        setStatic(obj.optBoolean("isStatic", false));
        return this;
    }
}
