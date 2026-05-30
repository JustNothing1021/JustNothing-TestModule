package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("Gc")
public class GcRequest extends CommandRequest {

    @CmdParam(
        name = "--full",
        description = "执行完整的GC",
        required = false,
        aliases = {"-f"}
    )
    private boolean fullGc = false;

    @CmdParam(
        name = "--stats",
        description = "显示GC统计信息",
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
