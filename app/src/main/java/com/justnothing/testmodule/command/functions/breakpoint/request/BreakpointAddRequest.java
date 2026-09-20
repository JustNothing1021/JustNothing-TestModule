package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointTexts;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;

public class BreakpointAddRequest extends CommandRequest<BreakpointResult> {

    @CmdParam(name = "className", position = 1, required = true, description = BreakpointTexts.PARAM_BREAKPOINT_ADD_CLASSNAME_DESC)
    private String className;

    @CmdParam(name = "methodName", position = 2, required = true, description = BreakpointTexts.PARAM_BREAKPOINT_ADD_METHODNAME_DESC)
    private String methodName;

    @CmdParam(name = "signature", aliases = {"sig", "signature"}, required = false, description = BreakpointTexts.PARAM_BREAKPOINT_ADD_SIGNATURE_DESC)
    private String signature;

    public BreakpointAddRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
