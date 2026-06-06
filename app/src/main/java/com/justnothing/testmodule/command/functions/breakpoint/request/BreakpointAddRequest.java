package com.justnothing.testmodule.command.functions.breakpoint.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("breakpoint:add")
public class BreakpointAddRequest extends CommandRequest {

    @CmdParam(name = "className", position = 1, required = true, description = "类名")
    private String className;

    @CmdParam(name = "methodName", position = 2, required = true, description = "方法名")
    private String methodName;

    @CmdParam(name = "signature", aliases = {"sig", "signature"}, required = false, description = "方法签名")
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
