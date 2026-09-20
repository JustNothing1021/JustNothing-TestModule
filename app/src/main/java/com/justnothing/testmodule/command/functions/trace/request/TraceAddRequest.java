package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.trace.response.TraceResult;
import com.justnothing.testmodule.command.functions.trace.TraceTexts;

public class TraceAddRequest extends CommandRequest<TraceResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = TraceTexts.PARAM_TRACE_ADD_CLASSNAME_DESC,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = true,
        description = TraceTexts.PARAM_TRACE_ADD_METHODNAME_DESC,
        serializedName = "methodName"
    )
    private String methodName;

    @CmdParam(
        name = "--sig",
        aliases = {"--signature"},
        required = false,
        description = TraceTexts.PARAM_TRACE_ADD_SIG_DESC
    )
    private String signature;

    public TraceAddRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
