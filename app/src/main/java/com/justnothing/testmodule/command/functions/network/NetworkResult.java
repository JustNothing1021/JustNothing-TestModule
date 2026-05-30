package com.justnothing.testmodule.command.functions.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class NetworkResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("hostname")
    private String hostname;
    @Expose @SerializedName("ipAddress")
    private String ipAddress;
    @Expose @SerializedName("port")
    private Integer port;
    @Expose @SerializedName("reachable")
    private Boolean reachable;
    @Expose @SerializedName("latencyMs")
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
}
