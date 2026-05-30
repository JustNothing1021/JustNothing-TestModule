package com.justnothing.testmodule.command.functions.hook.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONObject;

@SerializeKeyName("hook:add")
public class HookAddRequest extends CommandRequest {

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

    @Override
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("methodName", methodName);
        if (signature != null && !signature.isEmpty()) {
            obj.put("signature", signature);
        }
        if (beforeCode != null) obj.put("beforeCode", beforeCode);
        if (afterCode != null) obj.put("afterCode", afterCode);
        if (replaceCode != null) obj.put("replaceCode", replaceCode);
        if (beforeCodebase != null) obj.put("beforeCodebase", beforeCodebase);
        if (afterCodebase != null) obj.put("afterCodebase", afterCodebase);
        if (replaceCodebase != null) obj.put("replaceCodebase", replaceCodebase);
        return obj;
    }

    @Override
    public HookAddRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className", ""));
        setMethodName(obj.optString("methodName", ""));
        setSignature(obj.optString("signature", null));
        setBeforeCode(obj.optString("beforeCode", null));
        setAfterCode(obj.optString("afterCode", null));
        setReplaceCode(obj.optString("replaceCode", null));
        setBeforeCodebase(obj.optString("beforeCodebase", null));
        setAfterCodebase(obj.optString("afterCodebase", null));
        setReplaceCodebase(obj.optString("replaceCodebase", null));
        return this;
    }
}
