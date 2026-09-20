package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.PerfHookResult;

public class PerfHookStartRequest extends PerformanceRequest<PerfHookResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_HOOK_START_CLASSNAME_DESC
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_HOOK_START_METHODNAME_DESC
    )
    private String methodName;

    @CmdParam(
        name = "signature",
        position = 3,
        required = false,
        varArgs = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_HOOK_START_SIGNATURE_DESC
    )
    private String signature;

    public PerfHookStartRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
