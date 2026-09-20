package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeSourceRequest extends CommandRequest<BytecodeResult> {

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
        description = "把反编译结果另存到该文件或目录（不填则只打印）"
    )
    private String outputPath;

    @CmdParam(
        name = "highlight",
        aliases = {"--highlight"},
        required = false,
        description = "强制语法高亮（忽略规模上限；大段代码在手表上会卡顿）"
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
