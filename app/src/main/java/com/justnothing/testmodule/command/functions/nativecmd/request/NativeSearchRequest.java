package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("native:search")
public class NativeSearchRequest extends CommandRequest {

    @CmdParam(name = "pattern", required = true, description = "搜索模式")
    private String pattern;

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}
