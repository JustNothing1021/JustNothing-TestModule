package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NativeSearchRequest extends CommandRequest<NativeResult> {

    @CmdParam(name = "pattern", required = true, description = "搜索模式")
    private String pattern;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}
