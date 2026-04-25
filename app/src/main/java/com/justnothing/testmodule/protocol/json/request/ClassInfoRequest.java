package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class ClassInfoRequest extends CommandRequest {
    
    private String className;
    private boolean showInterfaces = true;
    private boolean showConstructors = true;
    private boolean showSuper = true;
    private boolean showModifiers = true;
    private boolean showMethods = true;
    private boolean showFields = true;
    
    public ClassInfoRequest() {
        super();
    }
    
    public ClassInfoRequest(String className) {
        super();
        this.className = className;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public boolean isShowInterfaces() {
        return showInterfaces;
    }
    
    public void setShowInterfaces(boolean showInterfaces) {
        this.showInterfaces = showInterfaces;
    }
    
    public boolean isShowConstructors() {
        return showConstructors;
    }
    
    public void setShowConstructors(boolean showConstructors) {
        this.showConstructors = showConstructors;
    }
    
    public boolean isShowSuper() {
        return showSuper;
    }
    
    public void setShowSuper(boolean showSuper) {
        this.showSuper = showSuper;
    }
    
    public boolean isShowModifiers() {
        return showModifiers;
    }
    
    public void setShowModifiers(boolean showModifiers) {
        this.showModifiers = showModifiers;
    }
    
    public boolean isShowMethods() {
        return showMethods;
    }
    
    public void setShowMethods(boolean showMethods) {
        this.showMethods = showMethods;
    }
    
    public boolean isShowFields() {
        return showFields;
    }
    
    public void setShowFields(boolean showFields) {
        this.showFields = showFields;
    }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("showInterfaces", showInterfaces);
        obj.put("showConstructors", showConstructors);
        obj.put("showSuper", showSuper);
        obj.put("showModifiers", showModifiers);
        obj.put("showMethods", showMethods);
        obj.put("showFields", showFields);
        return obj;
    }
    
    @Override
    public ClassInfoRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        this.setClassName(obj.optString("className"));
        this.setShowInterfaces(obj.optBoolean("showInterfaces", true));
        this.setShowConstructors(obj.optBoolean("showConstructors", true));
        this.setShowSuper(obj.optBoolean("showSuper", true));
        this.setShowModifiers(obj.optBoolean("showModifiers", true));
        this.setShowMethods(obj.optBoolean("showMethods", true));
        this.setShowFields(obj.optBoolean("showFields", true));
        return this;
    }
}
