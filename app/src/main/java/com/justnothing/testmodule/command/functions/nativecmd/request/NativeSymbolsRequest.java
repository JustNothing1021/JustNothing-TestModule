package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("native:symbols")
public class NativeSymbolsRequest extends CommandRequest {

    @CmdParam(name = "libName", required = true, description = "库名")
    private String libName;

    public String getLibName() { return libName; }
    public void setLibName(String libName) { this.libName = libName; }
}
