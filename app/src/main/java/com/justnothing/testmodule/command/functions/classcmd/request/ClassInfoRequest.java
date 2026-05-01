package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class ClassInfoRequest extends ClassCommandRequest {
    
    private String className;
    private boolean showInterfaces = true;
    private boolean showConstructors = true;
    private boolean showSuper = true;
    private boolean showModifiers = true;
    private boolean showMethods = true;
    private boolean showFields = true;
    private boolean showAll = true;
    private boolean isVerbose = false;

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

    public boolean isShowAll() {
        return showAll;
    }

    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
    }

    public boolean isVerbose() {
        return isVerbose;
    }

    public void setVerbose(boolean isVerbose) {
        this.isVerbose = isVerbose;
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
        obj.put("showAll", showAll);
        obj.put("isVerbose", isVerbose);
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
        this.setShowAll(obj.optBoolean("showAll", true));
        this.setVerbose(obj.optBoolean("isVerbose", false));
        return this;
    }

    @Override
    public ClassInfoRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException
                    ("参数不足, 需要至少1个参数: class info <class_name>");
        }

        boolean showInterfaces = false;
        boolean showConstructors = false;
        boolean showSuper = false;
        boolean showModifiers = false;
        boolean showAll = true;
        boolean verbose = false;

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v", "--verbose" -> verbose = true;
                case "-i", "--interfaces" -> {
                    showInterfaces = true;
                    showAll = false;
                }
                case "-c", "--constructors" -> {
                    showConstructors = true;
                    showAll = false;
                }
                case "-s", "--super" -> {
                    showSuper = true;
                    showAll = false;
                }
                case "-m", "--modifiers" -> {
                    showModifiers = true;
                    showAll = false;
                }
                case "-a", "--all" -> showAll = true;
            }
        }

        className = args[args.length - 1];
        this.setShowInterfaces(showInterfaces);
        this.setShowConstructors(showConstructors);
        this.setShowSuper(showSuper);
        this.setShowModifiers(showModifiers);
        this.setShowAll(showAll);
        this.setVerbose(verbose);
        return this;
    }
}
