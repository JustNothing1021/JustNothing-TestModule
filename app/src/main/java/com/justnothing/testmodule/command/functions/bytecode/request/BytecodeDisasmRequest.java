package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeDisasmRequest extends CommandRequest<BytecodeResult> {

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
        required = false,
        description = "只看这一个方法（不填则输出该类的全部方法）"
    )
    private String methodName;

    @CmdParam(
        name = "outputPath",
        aliases = {"-o", "--output"},
        required = false,
        description = "把指令写入文件（不填则直接显示，最多 150 行）"
    )
    private String outputPath;

    public BytecodeDisasmRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
}
