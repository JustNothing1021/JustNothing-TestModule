package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class WatchRequest extends CommandRequest {

    public static final String SUB_ADD = "add";
    public static final String SUB_LIST = "list";
    public static final String SUB_STOP = "stop";
    public static final String SUB_CLEAR = "clear";
    public static final String SUB_OUTPUT = "output";

    private String subCommand;
    private String targetType;
    private String className;
    private String memberName;
    private String signature;
    private Integer interval;
    private String watchId;
    private Integer outputCount;

    public WatchRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public Integer getInterval() { return interval; }
    public void setInterval(Integer interval) { this.interval = interval; }

    public String getWatchId() { return watchId; }
    public void setWatchId(String watchId) { this.watchId = watchId; }

    public Integer getOutputCount() { return outputCount; }
    public void setOutputCount(Integer outputCount) { this.outputCount = outputCount; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (targetType != null) obj.put("targetType", targetType);
        if (className != null) obj.put("className", className);
        if (memberName != null) obj.put("memberName", memberName);
        if (signature != null) obj.put("signature", signature);
        if (interval != null) obj.put("interval", interval);
        if (watchId != null) obj.put("watchId", watchId);
        if (outputCount != null) obj.put("outputCount", outputCount);
        return obj;
    }

    @Override
    public WatchRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", SUB_LIST));
        setTargetType(obj.optString("targetType", null));
        setClassName(obj.optString("className", null));
        setMemberName(obj.optString("memberName", null));
        setSignature(obj.optString("signature", null));
        setInterval(obj.has("interval") ? obj.getInt("interval") : null);
        setWatchId(obj.optString("watchId", null));
        setOutputCount(obj.has("outputCount") ? obj.getInt("outputCount") : null);
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        int pos = 1;
        if (args.length > pos && ("field".equals(args[pos]) || "method".equals(args[pos]))) {
            targetType = args[pos++];
        }
        if (args.length > pos && !isOption(args[pos])) className = args[pos++];
        if (args.length > pos && !isOption(args[pos])) memberName = args[pos++];
        for (int i = pos; i < args.length; i++) {
            String arg = args[i];
            try {
                interval = Integer.parseInt(arg);
                continue;
            } catch (NumberFormatException ignored) {}
            if ("sig".equals(arg) || "signature".equals(arg)) {
                if (i + 1 < args.length) signature = args[++i];
            }
        }
        if ((SUB_STOP.equals(subCommand) || SUB_OUTPUT.equals(subCommand)) && args.length > 1) {
            watchId = args[1];
        }
        if (SUB_OUTPUT.equals(subCommand) && args.length > 2) {
            try { outputCount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
        }
        return this;
    }

    private boolean isOption(String s) {
        return s.startsWith("-") || "field".equals(s) || "method".equals(s)
                || "sig".equals(s) || "signature".equals(s);
    }
}
