package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class AnalyzeClassRequest extends ClassCommandRequest {

    private String className;
    private boolean showFields = true;
    private boolean showMethods = true;
    private boolean showConstructors = true;
    private boolean showInterfaces = true;
    private boolean showSuper = true;
    private boolean showModifiers = true;
    private boolean showAll = true;
    private boolean isVerbose = false;
    private boolean showHierarchy = true;
    private boolean showStats = true;
    private boolean rawOutput = false;

    public AnalyzeClassRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isShowFields() { return showFields; }
    public void setShowFields(boolean showFields) { this.showFields = showFields; }
    public boolean isShowMethods() { return showMethods; }
    public void setShowMethods(boolean showMethods) { this.showMethods = showMethods; }
    public boolean isShowConstructors() { return showConstructors; }
    public void setShowConstructors(boolean showConstructors) { this.showConstructors = showConstructors; }
    public boolean isShowInterfaces() { return showInterfaces; }
    public void setShowInterfaces(boolean showInterfaces) { this.showInterfaces = showInterfaces; }
    public boolean isShowSuper() { return showSuper; }
    public void setShowSuper(boolean showSuper) { this.showSuper = showSuper; }
    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) { this.showModifiers = showModifiers; }
    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }
    public boolean isVerbose() { return isVerbose; }
    public void setVerbose(boolean verbose) { isVerbose = verbose; }
    public boolean isShowHierarchy() { return showHierarchy; }
    public void setShowHierarchy(boolean showHierarchy) { this.showHierarchy = showHierarchy; }
    public boolean isShowStats() { return showStats; }
    public void setShowStats(boolean showStats) { this.showStats = showStats; }
    public boolean isRawOutput() { return rawOutput; }
    public void setRawOutput(boolean rawOutput) { this.rawOutput = rawOutput; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("showFields", showFields);
        obj.put("showMethods", showMethods);
        obj.put("showConstructors", showConstructors);
        obj.put("showInterfaces", showInterfaces);
        obj.put("showSuper", showSuper);
        obj.put("showModifiers", showModifiers);
        obj.put("showAll", showAll);
        obj.put("isVerbose", isVerbose);
        obj.put("showHierarchy", showHierarchy);
        obj.put("showStats", showStats);
        obj.put("rawOutput", rawOutput);
        return obj;
    }

    @Override
    public AnalyzeClassRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setShowFields(obj.optBoolean("showFields", true));
        setShowMethods(obj.optBoolean("showMethods", true));
        setShowConstructors(obj.optBoolean("showConstructors", true));
        setShowInterfaces(obj.optBoolean("showInterfaces", true));
        setShowSuper(obj.optBoolean("showSuper", true));
        setShowModifiers(obj.optBoolean("showModifiers", true));
        setShowAll(obj.optBoolean("showAll", true));
        setVerbose(obj.optBoolean("isVerbose", false));
        setShowHierarchy(obj.optBoolean("showHierarchy", true));
        setShowStats(obj.optBoolean("showStats", true));
        setRawOutput(obj.optBoolean("rawOutput", false));
        return this;
    }

    @Override
    public AnalyzeClassRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("参数不足: class analyze [options] <class>");
        }

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "-v", "--verbose" -> isVerbose = true;
                case "-f", "--fields" -> { showFields = true; showAll = false; }
                case "-m", "--methods" -> { showMethods = true; showAll = false; }
                case "-c", "--constructors" -> { showConstructors = true; showAll = false; }
                case "-i", "--interfaces" -> { showInterfaces = true; showAll = false; }
                case "-s", "--super" -> { showSuper = true; showAll = false; }
                case "--modifiers" -> { showModifiers = true; showAll = false; }
                case "--hierarchy" -> showHierarchy = true;
                case "--stats" -> showStats = true;
                case "--raw" -> rawOutput = true;
                case "-a", "--all" -> showAll = true;
            }
        }

        className = args[args.length - 1];
        return this;
    }
}
