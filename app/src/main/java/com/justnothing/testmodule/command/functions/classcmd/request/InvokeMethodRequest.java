package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InvokeMethodRequest extends ClassCommandRequest {
    
    private String className;
    private String methodName;
    private String signature;
    private String targetInstance;
    private List<String> params;
    private List<String> paramTypes;
    private boolean freeMode;
    private boolean isStatic;
    private boolean accessSuper;
    private boolean accessInterfaces;
    
    public InvokeMethodRequest() {
        super();
        this.params = new ArrayList<>();
        this.paramTypes = new ArrayList<>();
    }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }
    public List<String> getParams() { return params; }
    public void setParams(List<String> params) { this.params = params; }
    public List<String> getParamTypes() { return paramTypes; }
    public void setParamTypes(List<String> paramTypes) { this.paramTypes = paramTypes; }
    public boolean isFreeMode() { return freeMode; }
    public void setFreeMode(boolean freeMode) { this.freeMode = freeMode; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }
    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("methodName", methodName);
        obj.put("signature", signature);
        obj.put("targetInstance", targetInstance);
        obj.put("freeMode", freeMode);
        obj.put("isStatic", isStatic);
        obj.put("accessSuper", accessSuper);
        obj.put("accessInterfaces", accessInterfaces);
        
        JSONArray paramsArray = new JSONArray();
        for (String param : params) {
            paramsArray.put(param);
        }
        obj.put("params", paramsArray);
        
        JSONArray paramTypesArray = new JSONArray();
        for (String paramType : paramTypes) {
            paramTypesArray.put(paramType != null ? paramType : "");
        }
        obj.put("paramTypes", paramTypesArray);
        
        return obj;
    }
    
    @Override
    public InvokeMethodRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setMethodName(obj.optString("methodName"));
        setSignature(obj.optString("signature"));
        setTargetInstance(obj.optString("targetInstance", null));
        setFreeMode(obj.optBoolean("freeMode", false));
        setStatic(obj.optBoolean("isStatic", false));
        setAccessSuper(obj.optBoolean("accessSuper", false));
        setAccessInterfaces(obj.optBoolean("accessInterfaces", false));

        params = new ArrayList<>();
        if (obj.has("params")) {
            JSONArray arr = obj.getJSONArray("params");
            for (int i = 0; i < arr.length(); i++) {
                params.add(arr.optString(i));
            }
        }

        paramTypes = new ArrayList<>();
        if (obj.has("paramTypes")) {
            JSONArray arr = obj.getJSONArray("paramTypes");
            for (int i = 0; i < arr.length(); i++) {
                String type = arr.optString(i, "");
                paramTypes.add(type.isEmpty() ? null : type);
            }
        }

        return this;
    }

    @Override
    public InvokeMethodRequest fromCommandLine(String[] args) {
        if (args.length > 0) className = args[0];
        if (args.length > 1) methodName = args[1];
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if ("-s".equals(arg)) isStatic = true;
            else if ("-f".equals(arg) || "--free".equals(arg)) freeMode = true;
            else if ("--super".equals(arg)) accessSuper = true;
            else if ("--interfaces".equals(arg)) accessInterfaces = true;
            else if (!arg.startsWith("-")) params.add(arg);
        }
        return this;
    }
}
