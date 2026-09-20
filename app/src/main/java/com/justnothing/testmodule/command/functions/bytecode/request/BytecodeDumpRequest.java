package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeDumpRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = BytecodeTexts.PARAM_BYTECODE_DUMP_CLASSNAME_DESC
    )
    private String className;

    @CmdParam(
        name = "outputPath",
        aliases = {"-o", "--output"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_DUMP_OUTPUTPATH_DESC
    )
    private String outputPath;

    @CmdParam(
        name = "disasm",
        aliases = {"-d", "--disasm"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_DUMP_DISASM_DESC
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
