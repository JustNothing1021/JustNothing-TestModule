package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeDumpRequest extends CommandRequest<BytecodeResult> {

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
        description = "输出目录（不填则自动挑一个可写目录）"
    )
    private String outputPath;

    @CmdParam(
        name = "disasm",
        aliases = {"-d", "--disasm"},
        required = false,
        description = "导出后用设备自带 dexdump 反汇编该类的方法"
    )
    private boolean disasm;

    public BytecodeDumpRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public boolean isDisasm() { return disasm; }
    public void setDisasm(boolean disasm) { this.disasm = disasm; }
}
