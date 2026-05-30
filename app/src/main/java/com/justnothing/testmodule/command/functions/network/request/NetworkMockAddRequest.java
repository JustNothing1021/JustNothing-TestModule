package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("network:mock:add")
public class NetworkMockAddRequest extends CommandRequest {

    @CmdParam(
        name = "pattern",
        position = 1,
        required = true,
        description = "URL 匹配模式",
        serializedName = "pattern"
    )
    private String pattern;

    @CmdParam(
        name = "response",
        position = 2,
        required = true,
        description = "Mock 响应内容",
        serializedName = "response"
    )
    private String response;

    @CmdParam(
        name = "--status",
        required = false,
        defaultValue = "200",
        description = "响应状态码",
        serializedName = "statusCode"
    )
    private int statusCode = 200;

    public NetworkMockAddRequest() {
        super();
    }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (pattern != null) obj.put("pattern", pattern);
        if (response != null) obj.put("response", response);
        obj.put("statusCode", statusCode);
        return obj;
    }

    @Override
    public NetworkMockAddRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setPattern(obj.optString("pattern", null));
        setResponse(obj.optString("response", null));
        setStatusCode(obj.optInt("statusCode", 200));
        return this;
    }
}
