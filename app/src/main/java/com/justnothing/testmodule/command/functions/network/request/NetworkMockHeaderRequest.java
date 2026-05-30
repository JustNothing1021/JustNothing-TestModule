package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("network:mock:header")
public class NetworkMockHeaderRequest extends CommandRequest {

    @CmdParam(
        name = "pattern",
        position = 1,
        required = true,
        description = "Mock 规则匹配模式",
        serializedName = "pattern"
    )
    private String pattern;

    @CmdParam(
        name = "name",
        position = 2,
        required = true,
        description = "响应头名称",
        serializedName = "headerName"
    )
    private String headerName;

    @CmdParam(
        name = "value",
        position = 3,
        required = true,
        description = "响应头值",
        serializedName = "headerValue"
    )
    private String headerValue;

    public NetworkMockHeaderRequest() {
        super();
    }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }

    public String getHeaderValue() { return headerValue; }
    public void setHeaderValue(String headerValue) { this.headerValue = headerValue; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (pattern != null) obj.put("pattern", pattern);
        if (headerName != null) obj.put("headerName", headerName);
        if (headerValue != null) obj.put("headerValue", headerValue);
        return obj;
    }

    @Override
    public NetworkMockHeaderRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setPattern(obj.optString("pattern", null));
        setHeaderName(obj.optString("headerName", null));
        setHeaderValue(obj.optString("headerValue", null));
        return this;
    }
}
