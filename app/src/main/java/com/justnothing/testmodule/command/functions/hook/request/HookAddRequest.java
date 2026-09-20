package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.hook.HookTexts;
import com.justnothing.testmodule.command.functions.hook.response.HookListResult;

public class HookAddRequest extends CommandRequest<HookListResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = HookTexts.PARAM_HOOK_ADD_CLASSNAME_DESC,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = true,
        description = HookTexts.PARAM_HOOK_ADD_METHODNAME_DESC,
        serializedName = "methodName"
    )
    private String methodName;

    @CmdParam(
        name = "--sig",
        aliases = {"--signature"},
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_SIG_DESC,
        serializedName = "signature"
    )
    private String signature;

    @CmdParam(
        name = "--before-code",
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_BEFORE_CODE_DESC
    )
    private String beforeCode;

    @CmdParam(
        name = "--before-codebase",
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_BEFORE_CODEBASE_DESC
    )
    private String beforeCodebase;

    @CmdParam(
        name = "--after-code",
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_AFTER_CODE_DESC
    )
    private String afterCode;

    @CmdParam(
        name = "--after-codebase",
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_AFTER_CODEBASE_DESC
    )
    private String afterCodebase;

    @CmdParam(
        name = "--replace-code",
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_REPLACE_CODE_DESC
    )
    private String replaceCode;

    @CmdParam(
        name = "--replace-codebase",
        required = false,
        description = HookTexts.PARAM_HOOK_ADD_REPLACE_CODEBASE_DESC
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
