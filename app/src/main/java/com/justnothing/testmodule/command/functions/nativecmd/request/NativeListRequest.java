package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NativeListRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "pattern", required = false, description = "过滤模式")
    private String pattern;

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = "详细输出")
    private Boolean verbose;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
