package com.justnothing.testmodule.command.functions.trace.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("trace:add")
public class TraceAddRequest extends CommandRequest {

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
        description = "方法签名"
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("methodName", methodName);
        if (signature != null) obj.put("signature", signature);
        return obj;
    }

    @Override
    public TraceAddRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className", null));
        setMethodName(obj.optString("methodName", null));
        setSignature(obj.optString("signature", null));
        return this;
    }
}
