package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.MethodListResult;

public class MethodListRequest extends ClassCommandRequest<MethodListResult> {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--verbose",
        description = "显示详细信息（参数、异常等）",
        aliases = {"-v"},
        serializedName = "verbose"
    )
    private boolean verbose;

    public MethodListRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}
