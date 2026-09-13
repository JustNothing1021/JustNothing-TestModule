package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.NativeResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NativeCliRequest extends CommandRequest<NativeResult> {

    @CmdParam(name = "className", required = true, description = "类名")
    private String className;

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = "详细输出")
    private Boolean verbose;

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
