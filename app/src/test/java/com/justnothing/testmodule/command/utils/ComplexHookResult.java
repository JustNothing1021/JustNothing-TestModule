package com.justnothing.testmodule.command.utils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.List;
import java.util.Map;

@SerializeKeyName("ComplexHookResult")
@AutoSerializable
public class ComplexHookResult extends CommandResult {

    @Expose @SerializedName("hookId")
    private String hookId;

    @Expose @SerializedName("hookedClasses")
    private List<String> hookedClasses;

    @Expose @SerializedName("statistics")
    private Map<String, Object> statistics;

    @Expose @SerializedName("originalRequest")
    private ComplexHookRequest originalRequest;

    public String getHookId() { return hookId; }
    public List<String> getHookedClasses() { return hookedClasses; }
    public Map<String, Object> getStatistics() { return statistics; }
    public ComplexHookRequest getOriginalRequest() { return originalRequest; }

    public void setHookId(String hookId) { this.hookId = hookId; }
    public void setHookedClasses(List<String> hookedClasses) { this.hookedClasses = hookedClasses; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
    public void setOriginalRequest(ComplexHookRequest request) { this.originalRequest = request; }

    @Override
    public String getResultType() {
        return "complex-hook";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComplexHookResult)) return false;
        ComplexHookResult other = (ComplexHookResult) obj;
        return isSuccess() == other.isSuccess()
            && (hookId == null ? other.hookId == null : hookId.equals(other.hookId))
            && getMessage() == null ? other.getMessage() == null : getMessage().equals(other.getMessage())
            && (hookedClasses == null ? other.hookedClasses == null : hookedClasses.equals(other.hookedClasses));
    }
}
