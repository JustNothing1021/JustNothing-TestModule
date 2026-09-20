package com.justnothing.testmodule.command.functions.memory.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.memory.response.GcResult;

public class GcRequest extends CommandRequest<GcResult> {

    @CmdParam(
        name = "--full",
        description = MemoryTexts.PARAM_MEMORY_GC_FULL_DESC,
        required = false,
        aliases = {"-f"}
    )
    private boolean fullGc = false;

    @CmdParam(
        name = "--stats",
        description = MemoryTexts.PARAM_MEMORY_GC_STATS_DESC,
        required = false,
        aliases = {"-s"}
    )
    private boolean showStats = false;

    public GcRequest() {
        super();
    }

    public GcRequest(boolean fullGc) {
        super();
        this.fullGc = fullGc;
    }

    public boolean isFullGc() { return fullGc; }
    public void setFullGc(boolean fullGc) { this.fullGc = fullGc; }

    public boolean isShowStats() { return showStats; }
    public void setShowStats(boolean showStats) { this.showStats = showStats; }
}
