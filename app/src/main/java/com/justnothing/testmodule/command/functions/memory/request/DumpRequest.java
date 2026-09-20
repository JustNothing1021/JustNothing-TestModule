package com.justnothing.testmodule.command.functions.memory.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.memory.response.DumpResult;

public class DumpRequest extends CommandRequest<DumpResult> {

    @CmdParam(
        name = "--heap",
        description = MemoryTexts.PARAM_MEMORY_DUMP_HEAP_DESC,
        required = false,
        aliases = {"-h"}
    )
    private boolean heapOnly = false;

    @CmdParam(
        name = "--threads",
        description = MemoryTexts.PARAM_MEMORY_DUMP_THREADS_DESC,
        required = false
    )
    private boolean threadsOnly = false;

    @CmdParam(
        name = "--full",
        description = MemoryTexts.PARAM_MEMORY_DUMP_FULL_DESC,
        required = false,
        defaultValue = "true"
    )
    private boolean fullDump = true;

    @CmdParam(
        name = "filePath",
        description = MemoryTexts.PARAM_MEMORY_DUMP_FILE_PATH_DESC,
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
