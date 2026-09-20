package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.system.SystemTexts;
import com.justnothing.testmodule.command.functions.system.response.SystemInfoResult;

// key 必须等于服务端路由的完整路径。system 路由的 path 为空，
// fullPath 形如 "父路径:"（参见 CommandRouter.registerRoute），故这里带尾冒号。
public class SystemInfoRequest extends CommandRequest<SystemInfoResult> {

    @CmdParam(
        name = "--cpu",
        description = SystemTexts.PARAM_SYSTEM_CPU_DESC,
        required = false,
        aliases = {"-c"}
    )
    private boolean showCpu = false;

    @CmdParam(
        name = "--memory",
        description = SystemTexts.PARAM_SYSTEM_MEMORY_DESC,
        required = false,
        aliases = {"-m"}
    )
    private boolean showMemory = false;

    @CmdParam(
        name = "--os",
        description = SystemTexts.PARAM_SYSTEM_OS_DESC,
        required = false,
        aliases = {"-o"}
    )
    private boolean showOs = false;

    @CmdParam(
        name = "--props",
        description = SystemTexts.PARAM_SYSTEM_PROPS_DESC,
        required = false,
        aliases = {"-p"}
    )
    private boolean showProps = false;

    @CmdParam(
        name = "--all",
        description = SystemTexts.PARAM_SYSTEM_ALL_DESC,
        required = false,
        defaultValue = "true"
    )
    private boolean showAll = true;

    public SystemInfoRequest() {
        super();
    }

    public boolean isShowCpu() { return showCpu; }
    public void setShowCpu(boolean showCpu) { this.showCpu = showCpu; }

    public boolean isShowMemory() { return showMemory; }
    public void setShowMemory(boolean showMemory) { this.showMemory = showMemory; }

    public boolean isShowOs() { return showOs; }
    public void setShowOs(boolean showOs) { this.showOs = showOs; }

    public boolean isShowProps() { return showProps; }
    public void setShowProps(boolean showProps) { this.showProps = showProps; }

    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }
}
