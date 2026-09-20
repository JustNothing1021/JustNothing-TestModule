package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeInfoRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "libName", required = true, description = NativeTexts.PARAM_NATIVE_INFO_LIBNAME_DESC)
    private String libName;

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = NativeTexts.PARAM_NATIVE_INFO_VERBOSE_DESC)
    private Boolean verbose;

    public String getLibName() { return libName; }
    public void setLibName(String libName) { this.libName = libName; }

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
