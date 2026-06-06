package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("SystraceResult")
public class SystraceResult extends CommandResult {

    private int taskId;

    private String status;

    private int duration;

    private String outputFile;

    private String report;

    private String exportPath;

    public SystraceResult() {}
    public SystraceResult(String requestId) { super(requestId); }

    public int getTaskId() { return taskId; }
    public void setTaskId(int v) { taskId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public int getDuration() { return duration; }
    public void setDuration(int v) { duration = v; }
    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String v) { outputFile = v; }
    public String getReport() { return report; }
    public void setReport(String v) { report = v; }
    public String getExportPath() { return exportPath; }
    public void setExportPath(String v) { exportPath = v; }
}
