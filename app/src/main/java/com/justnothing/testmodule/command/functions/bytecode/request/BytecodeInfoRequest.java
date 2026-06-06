package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("bytecode:info")
public class BytecodeInfoRequest extends CommandRequest {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "类名"
    )
    private String className;

    @CmdParam(
        name = "verbose",
        aliases = {"-v", "--verbose"},
        required = false,
        description = "详细输出"
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
