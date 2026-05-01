package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceRequest extends CommandRequest {

    public static final String SUB_SAMPLE = "sample";
    public static final String SUB_STOP = "stop";
    public static final String SUB_REPORT = "report";
    public static final String SUB_EXPORT = "export";
    public static final String SUB_SYSTRACE = "systrace";
    public static final String SUB_TRACE = "trace";

    private String subCommand;
    private Long durationMs;
    private String samplerType;
    private Integer sampleId;
    private String outputFile;

    public PerformanceRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getSamplerType() { return samplerType; }
    public void setSamplerType(String samplerType) { this.samplerType = samplerType; }

    public Integer getSampleId() { return sampleId; }
    public void setSampleId(Integer sampleId) { this.sampleId = sampleId; }

    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String outputFile) { this.outputFile = outputFile; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (durationMs != null) obj.put("durationMs", durationMs);
        if (samplerType != null) obj.put("samplerType", samplerType);
        if (sampleId != null) obj.put("sampleId", sampleId);
        if (outputFile != null) obj.put("outputFile", outputFile);
        return obj;
    }

    @Override
    public PerformanceRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", SUB_SAMPLE));
        setDurationMs(obj.has("durationMs") ? obj.getLong("durationMs") : null);
        setSamplerType(obj.optString("samplerType", null));
        setSampleId(obj.has("sampleId") ? obj.getInt("sampleId") : null);
        setOutputFile(obj.optString("outputFile", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                try {
                    long val = Long.parseLong(arg);
                    if (durationMs == null) durationMs = val;
                    else if (sampleId == null) sampleId = (int) val;
                } catch (NumberFormatException e) {
                    if (outputFile == null && (SUB_EXPORT.equals(subCommand) || SUB_REPORT.equals(subCommand))) {
                        outputFile = arg;
                    } else if (samplerType == null) {
                        samplerType = arg;
                    }
                }
            }
        }
        return this;
    }
}
