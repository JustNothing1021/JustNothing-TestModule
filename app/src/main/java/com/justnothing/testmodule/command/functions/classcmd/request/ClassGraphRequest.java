package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class ClassGraphRequest extends ClassCommandRequest {

    private String className;
    private boolean showSubclasses = true;
    private boolean showInterfaces = true;
    private int maxDepth = 10;
    private boolean compactMode = false;

    public ClassGraphRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isShowSubclasses() { return showSubclasses; }
    public void setShowSubclasses(boolean showSubclasses) { this.showSubclasses = showSubclasses; }
    public boolean isShowInterfaces() { return showInterfaces; }
    public void setShowInterfaces(boolean showInterfaces) { this.showInterfaces = showInterfaces; }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    public boolean isCompactMode() { return compactMode; }
    public void setCompactMode(boolean compactMode) { this.compactMode = compactMode; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("showSubclasses", showSubclasses);
        obj.put("showInterfaces", showInterfaces);
        obj.put("maxDepth", maxDepth);
        obj.put("compactMode", compactMode);
        return obj;
    }

    @Override
    public ClassGraphRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setShowSubclasses(obj.optBoolean("showSubclasses", true));
        setShowInterfaces(obj.optBoolean("showInterfaces", true));
        setMaxDepth(obj.optInt("maxDepth", 10));
        setCompactMode(obj.optBoolean("compactMode", false));
        return this;
    }

    @Override
    public ClassGraphRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("参数不足: class graph [options] <class>");
        }

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--no-subclasses" -> showSubclasses = false;
                case "--no-interfaces" -> showInterfaces = false;
                case "--compact" -> compactMode = true;
                case "--depth" -> {
                    if (i + 1 < args.length - 1) {
                        try {
                            maxDepth = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalCommandLineArgumentException("无效的深度值: " + args[i]);
                        }
                    }
                }
            }
        }

        className = args[args.length - 1];
        return this;
    }
}
