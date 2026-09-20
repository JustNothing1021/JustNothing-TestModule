package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeHeapRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = NativeTexts.PARAM_NATIVE_HEAP_VERBOSE_DESC)
    private Boolean verbose;

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
