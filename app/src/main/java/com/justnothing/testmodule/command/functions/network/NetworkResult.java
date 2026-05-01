package com.justnothing.testmodule.command.functions.network;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkResult extends CommandResult {

    private String subCommand;
    private String output;
    private String hostname;
    private String ipAddress;
    private Integer port;
    private Boolean reachable;
    private Long latencyMs;

    public NetworkResult() { super(); }
    public NetworkResult(String requestId) { super(requestId); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public Boolean getReachable() { return reachable; }
    public void setReachable(Boolean reachable) { this.reachable = reachable; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (output != null) obj.put("output", output);
        if (hostname != null) obj.put("hostname", hostname);
        if (ipAddress != null) obj.put("ipAddress", ipAddress);
        if (port != null) obj.put("port", port);
        if (reachable != null) obj.put("reachable", reachable);
        if (latencyMs != null) obj.put("latencyMs", latencyMs);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        subCommand = obj.optString("subCommand", null);
        output = obj.optString("output", null);
        hostname = obj.optString("hostname", null);
        ipAddress = obj.optString("ipAddress", null);
        port = obj.has("port") ? obj.getInt("port") : null;
        reachable = obj.has("reachable") ? obj.getBoolean("reachable") : null;
        latencyMs = obj.has("latencyMs") ? obj.getLong("latencyMs") : null;
    }
}
