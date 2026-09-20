package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeSearchRequest extends CommandRequest<NativeResult> {

    @CmdParam(name = "pattern", required = true, description = NativeTexts.PARAM_NATIVE_SEARCH_PATTERN_DESC)
    private String pattern;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}
