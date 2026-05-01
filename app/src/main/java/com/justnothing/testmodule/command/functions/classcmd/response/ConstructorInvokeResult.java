package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import org.json.JSONObject;
import org.json.JSONException;

public class ConstructorInvokeResult extends ClassCommandResult {

    private String className;
    private MethodInfo constructorInfo;
    private Object instance;
    private String instanceType;
    private int instanceHashCode;
    private boolean success;

    public ConstructorInvokeResult() {
        super();
    }

    public ConstructorInvokeResult(String requestId) {
        super(requestId);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public MethodInfo getConstructorInfo() {
        return constructorInfo;
    }

    public void setConstructorInfo(MethodInfo constructorInfo) {
        this.constructorInfo = constructorInfo;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public int getInstanceHashCode() {
        return instanceHashCode;
    }

    public void setInstanceHashCode(int instanceHashCode) {
        this.instanceHashCode = instanceHashCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("success", success);

        if (constructorInfo != null) {
            obj.put("constructorInfo", constructorInfo.toJson());
        }

        if (instance != null) {
            obj.put("instance", instance.toString());
            obj.put("instanceType", instanceType != null ? instanceType : instance.getClass().getName());
            obj.put("instanceHashCode", instanceHashCode);
        } else if (success) {
            obj.put("instance", JSONObject.NULL);
        }

        return obj;
    }
}
