package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeDecompileRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "类名"
    )
    private String className;

    @CmdParam(
        name = "outputPath",
        aliases = {"-o", "--output"},
        required = false,
        description = "输出路径"
    )
    private String outputPath;

    public BytecodeDecompileRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
}
