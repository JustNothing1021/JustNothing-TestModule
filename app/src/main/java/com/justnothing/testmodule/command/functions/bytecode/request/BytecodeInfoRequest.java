package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeInfoRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = BytecodeTexts.PARAM_BYTECODE_INFO_CLASSNAME_DESC
    )
    private String className;

    @CmdParam(
        name = "verbose",
        aliases = {"-v", "--verbose"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_INFO_VERBOSE_DESC
    )
    private boolean verbose;

    public BytecodeInfoRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}
