package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class TraceRequest extends CommandRequest {

    public static final String SUB_ADD = "add";
    public static final String SUB_LIST = "list";
    public static final String SUB_SHOW = "show";
    public static final String SUB_EXPORT = "export";
    public static final String SUB_STOP = "stop";
    public static final String SUB_CLEAR = "clear";

    private String subCommand;
    private String className;
    private String methodName;
    private String signature;
    private String traceId;
    private String exportFile;

    public TraceRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getExportFile() { return exportFile; }
    public void setExportFile(String exportFile) { this.exportFile = exportFile; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (className != null) obj.put("className", className);
        if (methodName != null) obj.put("methodName", methodName);
        if (signature != null) obj.put("signature", signature);
        if (traceId != null) obj.put("traceId", traceId);
        if (exportFile != null) obj.put("exportFile", exportFile);
        return obj;
    }

    @Override
    public TraceRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", SUB_LIST));
        setClassName(obj.optString("className", null));
        setMethodName(obj.optString("methodName", null));
        setSignature(obj.optString("signature", null));
        setTraceId(obj.optString("traceId", null));
        setExportFile(obj.optString("exportFile", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        int pos = 1;
        if (args.length > pos && !isOption(args[pos])) className = args[pos++];
        if (args.length > pos && !isOption(args[pos])) methodName = args[pos++];
        for (int i = pos; i < args.length; i++) {
            String arg = args[i];
            if ("sig".equals(arg) || "signature".equals(arg)) {
                if (i + 1 < args.length) signature = args[++i];
            } else if (!isOption(arg) && SUB_EXPORT.equals(subCommand) && exportFile == null) {
                exportFile = arg;
            } else if (!isOption(arg) && (SUB_STOP.equals(subCommand) || SUB_SHOW.equals(subCommand))) {
                traceId = arg;
            }
        }
        return this;
    }

    private boolean isOption(String s) {
        return s.startsWith("-") || "sig".equals(s) || "signature".equals(s);
    }
}
