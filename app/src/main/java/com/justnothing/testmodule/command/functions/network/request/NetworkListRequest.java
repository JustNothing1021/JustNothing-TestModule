package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("network:list")
public class NetworkListRequest extends CommandRequest {

    @CmdParam(
        name = "--method",
        required = false,
        description = "按请求方法过滤 (GET/POST/...)"
    )
    private String method;

    @CmdParam(
        name = "--host",
        required = false,
        description = "按主机过滤"
    )
    private String host;

    @CmdParam(
        name = "--status",
        required = false,
        description = "按状态码过滤 (200, 404, 5xx)"
    )
    private String statusFilter;

    @CmdParam(
        name = "--limit",
        required = false,
        defaultValue = "20",
        description = "限制显示数量"
    )
    private int limit = 20;

    public NetworkListRequest() {
        super();
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (method != null) obj.put("method", method);
        if (host != null) obj.put("host", host);
        if (statusFilter != null) obj.put("statusFilter", statusFilter);
        obj.put("limit", limit);
        return obj;
    }

    @Override
    public NetworkListRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setMethod(obj.optString("method", null));
        setHost(obj.optString("host", null));
        setStatusFilter(obj.optString("statusFilter", null));
        setLimit(obj.optInt("limit", 20));
        return this;
    }
}
