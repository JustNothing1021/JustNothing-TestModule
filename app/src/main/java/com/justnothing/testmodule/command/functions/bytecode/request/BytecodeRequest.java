package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class BytecodeRequest extends CommandRequest {

    private String subCommand;
    private String className;
    private String methodName;
    private String outputPath;
    private boolean verbose;
    private boolean hexFormat;

    public BytecodeRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    public boolean isHexFormat() { return hexFormat; }
    public void setHexFormat(boolean hexFormat) { this.hexFormat = hexFormat; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (className != null) obj.put("className", className);
        if (methodName != null) obj.put("methodName", methodName);
        if (outputPath != null) obj.put("outputPath", outputPath);
        obj.put("verbose", verbose);
        obj.put("hexFormat", hexFormat);
        return obj;
    }

    @Override
    public BytecodeRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", null));
        setClassName(obj.optString("className", null));
        setMethodName(obj.optString("methodName", null));
        setOutputPath(obj.optString("outputPath", null));
        setVerbose(obj.optBoolean("verbose", false));
        setHexFormat(obj.optBoolean("hexFormat", false));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        if (args.length > 1) className = args[1];
        if (args.length > 2) methodName = args[2];
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("-v".equals(arg) || "--verbose".equals(arg)) verbose = true;
            else if ("-h".equals(arg) || "--hex".equals(arg)) hexFormat = true;
            else if (("-o".equals(arg) || "--output".equals(arg)) && i + 1 < args.length) outputPath = args[++i];
        }
        return this;
    }
}
