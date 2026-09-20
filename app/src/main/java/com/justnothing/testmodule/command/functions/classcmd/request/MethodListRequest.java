package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.response.MethodListResult;

public class MethodListRequest extends ClassCommandRequest<MethodListResult> {

    @CmdParam(
        name = "class",
        description = ClassTexts.PARAM_CLASS_LIST_CLASS_DESC,
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--verbose",
        description = ClassTexts.PARAM_CLASS_LIST_VERBOSE_DESC,
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
