package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeSymbolsRequest extends CommandRequest<CommandResult> {

    @CmdParam(name = "libName", required = true, description = NativeTexts.PARAM_NATIVE_SYMBOLS_LIBNAME_DESC)
    private String libName;

    public String getLibName() { return libName; }
    public void setLibName(String libName) { this.libName = libName; }
}
