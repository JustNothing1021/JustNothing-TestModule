package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("native:heap")
public class NativeHeapRequest extends CommandRequest {

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = "详细输出")
    private Boolean verbose;

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
