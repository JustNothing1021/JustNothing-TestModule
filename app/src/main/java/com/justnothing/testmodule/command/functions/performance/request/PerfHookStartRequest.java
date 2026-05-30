package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:hook:start")
public class PerfHookStartRequest extends PerformanceRequest {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "目标类名"
    )
    private String className;

    @CmdParam(
        name = "methodName",
        position = 2,
        required = false,
        description = "方法名"
    )
    private String methodName;

    @CmdParam(
        name = "signature",
        position = 3,
        required = false,
        varArgs = true,
        description = "方法签名"
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        if (methodName != null) obj.put("methodName", methodName);
        if (signature != null) obj.put("signature", signature);
        return obj;
    }

    @Override
    public PerfHookStartRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className", ""));
        setMethodName(obj.optString("methodName", null));
        setSignature(obj.optString("signature", null));
        return this;
    }
}
