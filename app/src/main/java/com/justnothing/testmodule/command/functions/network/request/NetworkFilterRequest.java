package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("network:filter")
public class NetworkFilterRequest extends CommandRequest {

    @CmdParam(
        name = "host",
        position = 1,
        required = true,
        description = "主机名",
        serializedName = "host"
    )
    private String host;

    public NetworkFilterRequest() {
        super();
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getHostPattern() { return host; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (host != null) obj.put("host", host);
        return obj;
    }

    @Override
    public NetworkFilterRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setHost(obj.optString("host", null));
        return this;
    }
}
