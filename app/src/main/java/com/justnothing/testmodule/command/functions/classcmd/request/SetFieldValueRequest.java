package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class SetFieldValueRequest extends ClassCommandRequest {
    
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
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("fieldName", fieldName);
        obj.put("targetInstance", targetInstance);
        obj.put("valueExpression", valueExpression);
        obj.put("valueTypeHint", valueTypeHint);
        obj.put("isStatic", isStatic);
        return obj;
    }
    
    @Override
    public SetFieldValueRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setFieldName(obj.optString("fieldName"));
        setTargetInstance(obj.optString("targetInstance", null));
        setValueExpression(obj.optString("valueExpression"));
        setValueTypeHint(obj.optString("valueTypeHint", null));
        setStatic(obj.optBoolean("isStatic", false));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) className = args[0];
        if (args.length > 1) fieldName = args[1];
        if (args.length > 2) valueExpression = args[2];
        for (String arg : args) {
            if ("-s".equals(arg) || "--static".equals(arg)) isStatic = true;
            else if (arg.startsWith("--type=")) valueTypeHint = arg.substring(7);
        }
        return this;
    }
}
