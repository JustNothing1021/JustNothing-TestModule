package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeSourceRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = BytecodeTexts.PARAM_BYTECODE_SOURCE_CLASSNAME_DESC
    )
    private String className;

    @CmdParam(
        name = "outputPath",
        aliases = {"-o", "--output"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_SOURCE_OUTPUTPATH_DESC
    )
    private String outputPath;

    @CmdParam(
        name = "highlight",
        aliases = {"--highlight"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_SOURCE_HIGHLIGHT_DESC
    )
    private boolean highlight;

    public BytecodeSourceRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public boolean isHighlight() { return highlight; }
    public void setHighlight(boolean highlight) { this.highlight = highlight; }
}
