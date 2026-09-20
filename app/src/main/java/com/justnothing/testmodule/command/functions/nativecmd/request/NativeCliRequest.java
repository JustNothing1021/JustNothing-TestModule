package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeCliRequest extends CommandRequest<NativeResult> {

    @CmdParam(name = "className", required = true, description = NativeTexts.PARAM_NATIVE_CLI_CLASSNAME_DESC)
    private String className;

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = NativeTexts.PARAM_NATIVE_CLI_VERBOSE_DESC)
    private Boolean verbose;

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
