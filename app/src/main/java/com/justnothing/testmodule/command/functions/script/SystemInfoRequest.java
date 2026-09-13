package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.system.SystemInfoResult;

// key 必须等于服务端路由的完整路径。system 路由的 path 为空，
// fullPath 形如 "父路径:"（参见 CommandRouter.registerRoute），故这里带尾冒号。
public class SystemInfoRequest extends CommandRequest<SystemInfoResult> {

    @CmdParam(
        name = "--cpu",
        description = "显示CPU信息",
        required = false,
        aliases = {"-c"}
    )
    private boolean showCpu = false;

    @CmdParam(
        name = "--memory",
        description = "显示内存信息",
        required = false,
        aliases = {"-m"}
    )
    private boolean showMemory = false;

    @CmdParam(
        name = "--os",
        description = "显示操作系统信息",
        required = false,
        aliases = {"-o"}
    )
    private boolean showOs = false;

    @CmdParam(
        name = "--props",
        description = "显示系统属性",
        required = false,
        aliases = {"-p"}
    )
    private boolean showProps = false;

    @CmdParam(
        name = "--all",
        description = "显示所有信息（默认）",
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
