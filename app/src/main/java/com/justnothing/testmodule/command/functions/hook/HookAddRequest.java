package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONObject;

public class HookAddRequest extends CommandRequest {

    private String className;
    private String methodName;
    private String signature;
    private String beforeCode;
    private String afterCode;
    private String replaceCode;
    private String beforeCodebase;
    private String afterCodebase;
    private String replaceCodebase;

    public HookAddRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getBeforeCode() { return beforeCode; }
    public void setBeforeCode(String beforeCode) { this.beforeCode = beforeCode; }

    public String getAfterCode() { return afterCode; }
    public void setAfterCode(String afterCode) { this.afterCode = afterCode; }

    public String getReplaceCode() { return replaceCode; }
    public void setReplaceCode(String replaceCode) { this.replaceCode = replaceCode; }

    public String getBeforeCodebase() { return beforeCodebase; }
    public void setBeforeCodebase(String beforeCodebase) { this.beforeCodebase = beforeCodebase; }

    public String getAfterCodebase() { return afterCodebase; }
    public void setAfterCodebase(String afterCodebase) { this.afterCodebase = afterCodebase; }

    public String getReplaceCodebase() { return replaceCodebase; }
    public void setReplaceCodebase(String replaceCodebase) { this.replaceCodebase = replaceCodebase; }

    @Override
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("methodName", methodName);
        if (signature != null && !signature.isEmpty()) {
            obj.put("signature", signature);
        }
        if (beforeCode != null) obj.put("beforeCode", beforeCode);
        if (afterCode != null) obj.put("afterCode", afterCode);
        if (replaceCode != null) obj.put("replaceCode", replaceCode);
        if (beforeCodebase != null) obj.put("beforeCodebase", beforeCodebase);
        if (afterCodebase != null) obj.put("afterCodebase", afterCodebase);
        if (replaceCodebase != null) obj.put("replaceCodebase", replaceCodebase);
        return obj;
    }

    @Override
    public HookAddRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className", ""));
        setMethodName(obj.optString("methodName", ""));
        setSignature(obj.optString("signature", null));
        setBeforeCode(obj.optString("beforeCode", null));
        setAfterCode(obj.optString("afterCode", null));
        setReplaceCode(obj.optString("replaceCode", null));
        setBeforeCodebase(obj.optString("beforeCodebase", null));
        setAfterCodebase(obj.optString("afterCodebase", null));
        setReplaceCodebase(obj.optString("replaceCodebase", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) className = args[0];
        if (args.length > 1) methodName = args[1];
        if (args.length > 2) signature = args[2];
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            if ("--before".equals(arg) && i + 1 < args.length) beforeCode = args[++i];
            else if ("--after".equals(arg) && i + 1 < args.length) afterCode = args[++i];
            else if ("--replace".equals(arg) && i + 1 < args.length) replaceCode = args[++i];
        }
        return this;
    }
}
