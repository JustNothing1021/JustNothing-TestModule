package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("native:search")
public class NativeSearchRequest extends CommandRequest {

    @CmdParam(name = "pattern", required = true, description = "搜索模式")
    private String pattern;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}
