package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("Dump")
public class DumpRequest extends CommandRequest {

    @CmdParam(
        name = "--heap",
        description = "只导出堆信息",
        required = false,
        aliases = {"-h"}
    )
    private boolean heapOnly = false;

    @CmdParam(
        name = "--threads",
        description = "只导出线程信息",
        required = false
    )
    private boolean threadsOnly = false;

    @CmdParam(
        name = "--full",
        description = "导出完整信息 (默认)",
        required = false,
        defaultValue = "true"
    )
    private boolean fullDump = true;

    @CmdParam(
        name = "filePath",
        description = "输出文件路径",
        required = false,
        position = 1
    )
    private String filePath;

    public DumpRequest() {
        super();
    }

    public boolean isHeapOnly() { return heapOnly; }
    public void setHeapOnly(boolean heapOnly) { this.heapOnly = heapOnly; }

    public boolean isThreadsOnly() { return threadsOnly; }
    public void setThreadsOnly(boolean threadsOnly) { this.threadsOnly = threadsOnly; }

    public boolean isFullDump() { return fullDump; }
    public void setFullDump(boolean fullDump) { this.fullDump = fullDump; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
