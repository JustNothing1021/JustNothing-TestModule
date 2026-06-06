package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("bytecode:method")
public class BytecodeMethodRequest extends CommandRequest {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "类名"
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = true,
        description = "方法名"
    )
    private String methodName;

    @CmdParam(
        name = "hexFormat",
        aliases = {"-h", "--hex"},
        required = false,
        description = "十六进制格式"
    )
    private boolean hexFormat;

    public BytecodeMethodRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public boolean isHexFormat() { return hexFormat; }
    public void setHexFormat(boolean hexFormat) { this.hexFormat = hexFormat; }
}
