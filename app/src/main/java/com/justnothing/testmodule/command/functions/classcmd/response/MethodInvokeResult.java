package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import org.json.JSONObject;
import org.json.JSONException;

public class MethodInvokeResult extends ClassCommandResult {

    private String className;
    private String methodName;
    private MethodInfo methodInfo;
    private Object returnValue;
    private String returnType;
    private int returnHashCode;
    private boolean isStatic;
    private boolean success;

    public MethodInvokeResult() {
        super();
    }

    public MethodInvokeResult(String requestId) {
        super(requestId);
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public MethodInfo getMethodInfo() { return methodInfo; }
    public void setMethodInfo(MethodInfo methodInfo) { this.methodInfo = methodInfo; }
    public Object getReturnValue() { return returnValue; }
    public void setReturnValue(Object returnValue) { this.returnValue = returnValue; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public int getReturnHashCode() { return returnHashCode; }
    public void setReturnHashCode(int returnHashCode) { this.returnHashCode = returnHashCode; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("methodName", methodName);
        obj.put("success", success);
        obj.put("isStatic", isStatic);

        if (methodInfo != null) {
            obj.put("methodInfo", methodInfo.toJson());
        }

        if (returnValue != null) {
            obj.put("returnValue", returnValue.toString());
            obj.put("returnType", returnType != null ? returnType : returnValue.getClass().getName());
            obj.put("returnHashCode", returnHashCode);
        } else {
            obj.put("returnValue", JSONObject.NULL);
        }

        return obj;
    }
}
