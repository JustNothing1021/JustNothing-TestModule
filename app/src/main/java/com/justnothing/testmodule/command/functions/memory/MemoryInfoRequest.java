package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("memory:info")
public class MemoryInfoRequest extends CommandRequest {

    public static final String LEVEL_BASIC = "basic";
    public static final String LEVEL_FULL = "full";

    @CmdParam(
        name = "--detail-level",
        description = "信息详细程度",
        required = false,
        defaultValue = "full",
        allowedValues = {"basic", "full"},
        serializedName = "detailLevel"
    )
    private String detailLevel = LEVEL_FULL;

    @CmdParam(
        name = "--heap",
        description = "只显示堆内存信息",
        required = false,
        aliases = {"-h"},
        serializedName = "heapOnly"
    )
    private boolean heapOnly = false;

    @CmdParam(
        name = "--detailed",
        description = "显示详细内存信息 (默认)",
        required = false,
        defaultValue = "true",
        aliases = {"-d"},
        serializedName = "detailed"
    )
    private boolean detailed = true;

    public MemoryInfoRequest() {
        super();
    }

    public MemoryInfoRequest(String detailLevel) {
        super();
        this.detailLevel = detailLevel;
    }

    public String getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(String detailLevel) {
        this.detailLevel = detailLevel;
    }

    public boolean isHeapOnly() {
        return heapOnly;
    }

    public void setHeapOnly(boolean heapOnly) {
        this.heapOnly = heapOnly;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(boolean detailed) {
        this.detailed = true;
    }
}
