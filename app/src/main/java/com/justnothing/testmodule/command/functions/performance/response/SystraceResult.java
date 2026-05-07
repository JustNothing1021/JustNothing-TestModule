package com.justnothing.testmodule.command.functions.performance.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

@SerializeKeyName("SystraceResult")
@AutoSerializable
public class SystraceResult extends CommandResult {

    @ResultField(name = "taskId", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int taskId;

    @ResultField(name = "status", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String status;

    @ResultField(name = "duration", description = "采集时长(秒)", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int duration;

    @ResultField(name = "outputFile", description = "原始输出文件路径", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String outputFile;

    @ResultField(name = "report", description = "解析后的报告文本", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String report;

    @ResultField(name = "exportPath", defaultValue = ValueSupplier.EmptyStringSupplier.class)
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
