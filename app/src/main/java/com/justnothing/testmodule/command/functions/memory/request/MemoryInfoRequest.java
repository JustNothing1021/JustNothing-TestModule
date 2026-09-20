package com.justnothing.testmodule.command.functions.memory.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.memory.response.MemoryInfoResult;

public class MemoryInfoRequest extends CommandRequest<MemoryInfoResult> {

    public static final String LEVEL_BASIC = "basic";
    public static final String LEVEL_FULL = "full";

    @CmdParam(
        name = "--detail-level",
        description = MemoryTexts.PARAM_MEMORY_INFO_DETAIL_LEVEL_DESC,
        required = false,
        defaultValue = "full",
        allowedValues = {"basic", "full"},
        serializedName = "detailLevel"
    )
    private String detailLevel = LEVEL_FULL;

    @CmdParam(
        name = "--heap",
        description = MemoryTexts.PARAM_MEMORY_INFO_HEAP_DESC,
        required = false,
        aliases = {"-h"},
        serializedName = "heapOnly"
    )
    private boolean heapOnly = false;

    @CmdParam(
        name = "--detailed",
        description = MemoryTexts.PARAM_MEMORY_INFO_DETAILED_DESC,
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
