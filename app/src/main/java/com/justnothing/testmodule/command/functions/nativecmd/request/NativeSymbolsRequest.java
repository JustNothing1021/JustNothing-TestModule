package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NativeSymbolsRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "libName", required = true, description = "库名")
    private String libName;

    public String getLibName() { return libName; }
    public void setLibName(String libName) { this.libName = libName; }
}
