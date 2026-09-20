package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.hook.response.HookListResult;

public class HookAddRequest extends CommandRequest<HookListResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "目标类名",
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = true,
        description = "目标方法名",
        serializedName = "methodName"
    )
    private String methodName;

    @CmdParam(
        name = "--sig",
        aliases = {"--signature"},
        required = false,
        description = "方法签名",
        serializedName = "signature"
    )
    private String signature;

    @CmdParam(
        name = "--before-code",
        required = false,
        description = "before阶段内联代码"
    )
    private String beforeCode;

    @CmdParam(
        name = "--before-codebase",
        required = false,
        description = "before阶段代码文件"
    )
    private String beforeCodebase;

    @CmdParam(
        name = "--after-code",
        required = false,
        description = "after阶段内联代码"
    )
    private String afterCode;

    @CmdParam(
        name = "--after-codebase",
        required = false,
        description = "after阶段代码文件"
    )
    private String afterCodebase;

    @CmdParam(
        name = "--replace-code",
        required = false,
        description = "replace阶段内联代码"
    )
    private String replaceCode;

    @CmdParam(
        name = "--replace-codebase",
        required = false,
        description = "replace阶段代码文件"
    )
    private String replaceCodebase;

    public HookAddRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getBeforeCode() { return beforeCode; }
    public void setBeforeCode(String beforeCode) { this.beforeCode = beforeCode; }

    public String getAfterCode() { return afterCode; }
    public void setAfterCode(String afterCode) { this.afterCode = afterCode; }

    public String getReplaceCode() { return replaceCode; }
    public void setReplaceCode(String replaceCode) { this.replaceCode = replaceCode; }

    public String getBeforeCodebase() { return beforeCodebase; }
    public void setBeforeCodebase(String beforeCodebase) { this.beforeCodebase = beforeCodebase; }

    public String getAfterCodebase() { return afterCodebase; }
    public void setAfterCodebase(String afterCodebase) { this.afterCodebase = afterCodebase; }

    public String getReplaceCodebase() { return replaceCodebase; }
    public void setReplaceCodebase(String replaceCodebase) { this.replaceCodebase = replaceCodebase; }
}
