package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeVerifyRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "类名"
    )
    private String className;

    public BytecodeVerifyRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}
