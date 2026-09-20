package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeListRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "pattern", required = false, description = NativeTexts.PARAM_NATIVE_LIST_PATTERN_DESC)
    private String pattern;

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = NativeTexts.PARAM_NATIVE_LIST_VERBOSE_DESC)
    private Boolean verbose;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
