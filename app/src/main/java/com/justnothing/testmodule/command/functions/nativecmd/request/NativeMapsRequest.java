package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NativeMapsRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = "详细输出")
    private Boolean verbose;

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
