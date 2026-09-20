package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeMethodRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = BytecodeTexts.PARAM_BYTECODE_METHOD_CLASSNAME_DESC
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = true,
        description = BytecodeTexts.PARAM_BYTECODE_METHOD_METHODNAME_DESC
    )
    private String methodName;

    public BytecodeMethodRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
}
