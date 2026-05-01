package com.justnothing.testmodule.command.functions.nativecmd;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class NativeRequest extends CommandRequest {

    public static final String SUB_LIST = "list";
    public static final String SUB_INFO = "info";
    public static final String SUB_CLI = "cli";
    public static final String SUB_SYMBOLS = "symbols";
    public static final String SUB_MEMORY = "memory";
    public static final String SUB_HEAP = "heap";
    public static final String SUB_STACK = "stack";
    public static final String SUB_MAPS = "maps";
    public static final String SUB_SEARCH = "search";

    private String subCommand;
    private String libraryName;
    private String className;
    private String pattern;
    private boolean verbose;
    private String threadId;

    public NativeRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (libraryName != null) obj.put("libraryName", libraryName);
        if (className != null) obj.put("className", className);
        if (pattern != null) obj.put("pattern", pattern);
        obj.put("verbose", verbose);
        if (threadId != null) obj.put("threadId", threadId);
        return obj;
    }

    @Override
    public NativeRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", SUB_LIST));
        setLibraryName(obj.optString("libraryName", null));
        setClassName(obj.optString("className", null));
        setPattern(obj.optString("pattern", null));
        setVerbose(obj.optBoolean("verbose", false));
        setThreadId(obj.optString("threadId", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        if (args.length > 1) {
            String firstArg = args[1];
            if (!firstArg.startsWith("-")) {
                switch (subCommand) {
                    case SUB_INFO, SUB_SYMBOLS -> libraryName = firstArg;
                    case SUB_CLI -> className = firstArg;
                    case SUB_LIST, SUB_SEARCH -> pattern = firstArg;
                    default -> {}
                }
            }
        }
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("-v".equals(arg) || "--verbose".equals(arg)) verbose = true;
            else if (("-t".equals(arg) || "--thread".equals(arg)) && i + 1 < args.length) threadId = args[++i];
        }
        return this;
    }
}
