package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InvokeConstructorRequest extends CommandRequest {
    
    private String className;
    private String signature;
    private List<String> params;
    private List<String> paramTypes;
    private boolean freeMode;
    
    public InvokeConstructorRequest() {
        super();
        this.params = new ArrayList<>();
        this.paramTypes = new ArrayList<>();
    }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public List<String> getParams() { return params; }
    public void setParams(List<String> params) { this.params = params; }
    public List<String> getParamTypes() { return paramTypes; }
    public void setParamTypes(List<String> paramTypes) { this.paramTypes = paramTypes; }
    public boolean isFreeMode() { return freeMode; }
    public void setFreeMode(boolean freeMode) { this.freeMode = freeMode; }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("requestId", getRequestId());
        obj.put("commandType", getCommandType());
        obj.put("className", className);
        obj.put("signature", signature);
        obj.put("freeMode", freeMode);
        
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
    public InvokeConstructorRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setSignature(obj.optString("signature"));
        setFreeMode(obj.optBoolean("freeMode", false));
        
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
}
