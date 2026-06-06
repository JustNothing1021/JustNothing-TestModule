package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("native:cli")
public class NativeCliRequest extends CommandRequest {

    @CmdParam(name = "className", required = true, description = "类名")
    private String className;

    @CmdParam(name = "verbose", aliases = {"-v", "--verbose"}, required = false, description = "详细输出")
    private Boolean verbose;

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Boolean getVerbose() { return verbose; }
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
}
